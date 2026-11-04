// Direct ADB transport to this local, unsecured SDK emulator only.
// Avoids starting a host ADB server or creating keys in the Windows profile.
const net=require('net'), fs=require('fs');
const [,,mode,...args]=process.argv;
const socket=net.createConnection({host:'127.0.0.1',port:5557});
let incoming=Buffer.alloc(0),remote=0,opened=false,sendQueue=[],waiting=false,result=[];
const timer=setTimeout(()=>{console.error('Emulator request timed out');socket.destroy();process.exitCode=1;},45000);
function frame(command,a,b,data=Buffer.alloc(0)){
    const h=Buffer.alloc(24),code=Buffer.from(command).readUInt32LE();
    h.writeUInt32LE(code,0);h.writeUInt32LE(a>>>0,4);h.writeUInt32LE(b>>>0,8);h.writeUInt32LE(data.length,12);
    h.writeUInt32LE(data.reduce((s,n)=>(s+n)>>>0,0),16);h.writeUInt32LE((~code)>>>0,20);socket.write(Buffer.concat([h,data]));
}
function flush(){if(!waiting&&sendQueue.length){waiting=true;frame('WRTE',1,remote,sendQueue.shift());}}
function queue(data){for(let i=0;i<data.length;i+=4096)sendQueue.push(data.subarray(i,i+4096));flush();}
function sync(id,value){const h=Buffer.alloc(8);h.write(id);h.writeUInt32LE(Buffer.isBuffer(value)?value.length:value,4);return Buffer.isBuffer(value)?Buffer.concat([h,value]):h;}
function finish(){clearTimeout(timer);socket.end();}
socket.on('connect',()=>frame('CNXN',0x01000000,4096,Buffer.from('host::\0')));
socket.on('error',e=>{clearTimeout(timer);console.error(e.message);process.exitCode=1;});
socket.on('data',data=>{
    incoming=Buffer.concat([incoming,data]);
    while(incoming.length>=24){
        const len=incoming.readUInt32LE(12);if(incoming.length<24+len)break;
        const command=incoming.toString('ascii',0,4),a=incoming.readUInt32LE(4),b=incoming.readUInt32LE(8),body=incoming.subarray(24,24+len);
        incoming=incoming.subarray(24+len);
        if(command==='AUTH'){console.error('Emulator requires authentication; refusing a non-local/unsecured target.');process.exitCode=1;finish();return;}
        if(command==='CNXN'){
            const service=mode==='push'?'sync:':((mode==='capture'?'exec:':'shell:')+(mode==='capture'?'screencap -p':args.join(' ')));
            frame('OPEN',1,0,Buffer.from(service+'\0'));
        }else if(command==='OKAY'){
            remote=a;
            if(!opened){opened=true;if(mode==='push'){
                const bytes=fs.readFileSync(args[0]),path=Buffer.from(args[1]+',33206');
                const chunks=[sync('SEND',path)];
                for(let i=0;i<bytes.length;i+=32768)chunks.push(sync('DATA',bytes.subarray(i,i+32768)));
                chunks.push(sync('DONE',Math.floor(Date.now()/1000)));queue(Buffer.concat(chunks));
            }}else{waiting=false;flush();}
        }else if(command==='WRTE'){
            frame('OKAY',1,a);result.push(Buffer.from(body));
            if(mode==='push'){
                const all=Buffer.concat(result);
                if(all.length>=8){if(all.toString('ascii',0,4)==='OKAY'){console.log('APK transferred to emulator');frame('CLSE',1,a);finish();}
                else if(all.toString('ascii',0,4)==='FAIL'){console.error(all.subarray(8).toString());process.exitCode=1;finish();}}
            }
        }else if(command==='CLSE'){
            frame('CLSE',1,a);const bytes=Buffer.concat(result);
            if(mode==='capture')fs.writeFileSync(args[0],bytes);else if(mode!=='push')process.stdout.write(bytes);
            finish();
        }
    }
});
