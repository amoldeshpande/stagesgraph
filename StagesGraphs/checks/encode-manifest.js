// Encode the resource-free application's manifest as Android binary XML.
// Attribute IDs and style IDs are read from the installed Android SDK.
const fs = require('fs');
const [,, input, attributes, styles, output] = process.argv;
const tree = JSON.parse(fs.readFileSync(input, 'utf8').replace(/^\uFEFF/, ''));
const read = p => { const b=fs.readFileSync(p); return b[0]===255&&b[1]===254?b.subarray(2).toString('utf16le'):b.toString('utf8'); };
const attrIds = Object.fromEntries([...read(attributes).matchAll(/int (\w+) = (\d+);/g)].map(m=>[m[1],+m[2]]));
const theme = +read(styles).match(/Theme_Material_NoActionBar = (\d+);/)[1];
const uri='http://schemas.android.com/apk/res/android';
const strings=[], ids=new Map();
const str=s=>{if(!ids.has(s)){ids.set(s,strings.length);strings.push(s);}return ids.get(s);};
const u16=n=>{let b=Buffer.alloc(2);b.writeUInt16LE(n);return b;};
const u32=n=>{let b=Buffer.alloc(4);b.writeUInt32LE(n>>>0);return b;};
const cat=(...b)=>Buffer.concat(b);
const header=(t,h,n)=>cat(u16(t),u16(h),u32(n));
const node=(t,extra)=>cat(header(t,16,16+extra.length),u32(1),u32(0xffffffff),extra);
str('android');str(uri);
function attrs(element){return (element.attributes||[]).map(a=>{
  str(a.name);if(a.ns)str(a.ns);str(a.value);
  let type=3,value=str(a.value),resource=0;
  if(a.ns){
    resource=attrIds[a.name];if(!resource)throw Error('Unknown Android attribute '+a.name);
    if(a.value==='true'||a.value==='false'){type=18;value=a.value==='true'?1:0;}
    else if(['minSdkVersion','targetSdkVersion','versionCode'].includes(a.name)){type=16;value=+a.value;}
    else if(a.name==='screenOrientation'){if(a.value!=='landscape')throw Error('Unsupported orientation');type=16;value=0;}
    else if(a.name==='theme'){if(a.value!=='@android:style/Theme.Material.NoActionBar')throw Error('Unsupported theme');type=1;value=theme;}
  }
  return {...a,resource,type,typed:value};
}).sort((a,b)=>a.resource-b.resource);}
const prepared=new Map();
function collect(e){str(e.name);prepared.set(e,attrs(e));for(const child of e.children||[])collect(child);}
collect(tree);
let offset=0;const offsets=[],encoded=[];
for(const s of strings){offsets.push(offset);const b=cat(u16(s.length),Buffer.from(s,'utf16le'),u16(0));encoded.push(b);offset+=b.length;}
const raw=cat(...encoded),padding=Buffer.alloc((4-raw.length%4)%4);
const start=28+4*strings.length;
const pool=cat(header(1,28,start+raw.length+padding.length),u32(strings.length),u32(0),u32(0),u32(start),u32(0),...offsets.map(u32),raw,padding);
const resources=strings.map(s=>attrIds[s]||0);
const map=cat(header(0x180,8,8+4*resources.length),...resources.map(u32));
const chunks=[pool,map,node(0x100,cat(u32(str('android')),u32(str(uri))))];
function emit(e){
  const aa=prepared.get(e);
  const encodedAttrs=aa.map(a=>cat(u32(a.ns?str(a.ns):0xffffffff),u32(str(a.name)),u32(str(a.value)),u16(8),Buffer.from([0,a.type]),u32(a.typed)));
  chunks.push(node(0x102,cat(u32(0xffffffff),u32(str(e.name)),u16(20),u16(20),u16(aa.length),u16(0),u16(0),u16(0),...encodedAttrs)));
  for(const child of e.children||[])emit(child);
  chunks.push(node(0x103,cat(u32(0xffffffff),u32(str(e.name)))));
}
emit(tree);chunks.push(node(0x101,cat(u32(str('android')),u32(str(uri)))));
const body=cat(...chunks);fs.writeFileSync(output,cat(header(3,8,8+body.length),body));
console.log('Binary manifest encoded from source XML.');
