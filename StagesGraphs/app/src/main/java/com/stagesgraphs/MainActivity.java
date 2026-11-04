package com.stagesgraphs;

import android.app.*;
import android.os.*;
import android.bluetooth.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    static UUID uuid(String id) { return UUID.fromString("0000"+id+"-0000-1000-8000-00805f9b34fb"); }
    static final UUID CCC = uuid("2902");
    final Handler ui = new Handler();
    BluetoothAdapter adapter;
    LinearLayout root;
    Graph heart, power;
    Sensor hr, watts;
    boolean demo, resumed;
    long started;
    TextView heading;
    final Random random = new Random();
    final Runnable tick = new Runnable() { public void run() {
        long now = SystemClock.elapsedRealtime();
        if (demo) {
            double t=(now-started)/1000.0;
            heart.add(135+12*Math.sin(t/17)+random.nextInt(3),now);
            power.add(210+65*Math.sin(t/9)+random.nextInt(15),now);
        }
        heart.invalidate(); power.invalidate();
        ui.postDelayed(this,1000);
    }};
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(1024,1024);
        adapter=((BluetoothManager)getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        root=new LinearLayout(this); root.setOrientation(1); root.setPadding(20,10,20,10); root.setBackgroundColor(Color.rgb(10,17,29));
        setContentView(root);
        heading=new TextView(this); heading.setTextColor(Color.WHITE); heading.setTextSize(20); heading.setText("STAGES GRAPHS  •  LIVE BLUETOOTH"); root.addView(heading);
        LinearLayout actions=new LinearLayout(this); root.addView(actions);
        heart=new Graph("HEART RATE","BPM",Color.rgb(255,106,139),220);
        power=new Graph("CYCLING POWER","W",Color.rgb(63,211,236),400);
        hr=new Sensor("Heart rate","180d","2a37",heart);
        watts=new Sensor("Power meter","1818","2a63",power);
        button(actions,"Connect heart rate",()->choose(hr));
        button(actions,"Connect power",()->choose(watts));
        button(actions,"Demo / Live",()->{
            demo=!demo; hr.stop(); watts.stop(); heart.clear(); power.clear(); started=SystemClock.elapsedRealtime();
            heading.setText(demo?"STAGES GRAPHS  •  DEMO — SIMULATED DATA":"STAGES GRAPHS  •  LIVE BLUETOOTH");
        });
        button(actions,"Clear graphs",()->{heart.clear();power.clear();});
        root.addView(heart,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(power,new LinearLayout.LayoutParams(-1,0,1));
    }
    void button(LinearLayout row,String label,Runnable action) {
        Button b=new Button(this); b.setText(label); b.setTextSize(13); row.addView(b,new LinearLayout.LayoutParams(0,-2,1)); b.setOnClickListener(v->action.run());
    }
    @Override public void onResume(){ super.onResume();resumed=true;ui.post(tick); }
    @Override public void onPause(){ super.onPause();resumed=false;ui.removeCallbacks(tick);stopScan();hr.stop();watts.stop(); }
    BluetoothAdapter.LeScanCallback scanner;
    Runnable scanTimeout;
    void stopScan(){if(scanner!=null&&adapter!=null){adapter.stopLeScan(scanner);scanner=null;}if(scanTimeout!=null)ui.removeCallbacks(scanTimeout);}
    void choose(final Sensor sensor) {
        if(demo){new AlertDialog.Builder(this).setMessage("Switch Demo / Live to live mode before connecting sensors.").setPositiveButton("OK",null).show();return;}
        if(adapter==null||!getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")){sensor.status("Bluetooth LE is unavailable on this console");return;}
        if(!adapter.isEnabled()){startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));return;}
        stopScan();
        final ArrayList<BluetoothDevice> devices=new ArrayList<>();
        final ArrayAdapter<String> rows=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1);
        final AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Scanning: "+sensor.label).setAdapter(rows,(d,index)->{
            stopScan();sensor.stop();sensor.device=devices.get(index);sensor.connect();
        }).setNegativeButton("Cancel",null).create();
        dialog.setOnDismissListener(d->stopScan()); dialog.show();
        scanner=(device,rssi,record)->ui.post(()->{
            if(!devices.contains(device)){devices.add(device);rows.add((device.getName()==null?"Unnamed Bluetooth device":device.getName())+"\n"+device.getAddress());}
        });
        if(!adapter.startLeScan(scanner)){dialog.setTitle("Scan failed — check Bluetooth");return;}
        scanTimeout=()->{stopScan();dialog.setTitle(devices.isEmpty()?"No sensors found — wake sensors and retry":"Select "+sensor.label);};
        ui.postDelayed(scanTimeout,15000);
    }
    class Sensor {
        String label; UUID service,characteristic; Graph graph; BluetoothDevice device; BluetoothGatt gatt;
        boolean active; int failures; Runnable retry,timeout;
        Sensor(String l,String s,String c,Graph v){label=l;service=uuid(s);characteristic=uuid(c);graph=v;}
        void status(String s){graph.status=s;graph.invalidate();}
        void cancelTimers(){if(retry!=null)ui.removeCallbacks(retry);if(timeout!=null)ui.removeCallbacks(timeout);}
        void stop(){active=false;cancelTimers();if(gatt!=null){BluetoothGatt old=gatt;gatt=null;old.disconnect();old.close();}status("Disconnected • tap Connect");}
        void connect(){
            if(!resumed||demo||device==null)return;
            active=true;status("Connecting to "+device.getName());
            gatt=device.connectGatt(MainActivity.this,false,callback);
            timeout=()->{if(active)failed("Connection timed out");};ui.postDelayed(timeout,20000);
        }
        void failed(String why){
            cancelTimers();if(gatt!=null){BluetoothGatt old=gatt;gatt=null;old.disconnect();old.close();}
            if(active&&resumed){int delay=Math.min(30,2+(failures++*3));status(why+" • retry in "+delay+"s");retry=()->connect();ui.postDelayed(retry,delay*1000L);}
        }
        final BluetoothGattCallback callback=new BluetoothGattCallback(){
            public void onConnectionStateChange(BluetoothGatt g,int status,int state){ui.post(()->{
                if(g!=gatt)return;
                if(status==BluetoothGatt.GATT_SUCCESS&&state==BluetoothProfile.STATE_CONNECTED){Sensor.this.status("Discovering services…");if(!g.discoverServices())failed("Service discovery failed");}
                else if(state==BluetoothProfile.STATE_DISCONNECTED||status!=0)failed("Disconnected ("+status+")");
            });}
            public void onServicesDiscovered(BluetoothGatt g,int result){ui.post(()->{
                if(g!=gatt)return;
                if(result!=0){failed("Service discovery failed");return;}
                BluetoothGattService s=g.getService(service);
                BluetoothGattCharacteristic c=s==null?null:s.getCharacteristic(characteristic);
                if(c==null){stop();Sensor.this.status("Selected device does not offer standard "+label+" data");return;}
                BluetoothGattDescriptor d=c.getDescriptor(CCC);
                if(d==null||!g.setCharacteristicNotification(c,true)){failed("Cannot enable sensor updates");return;}
                d.setValue((c.getProperties()&BluetoothGattCharacteristic.PROPERTY_NOTIFY)!=0?BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE:BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                if(!g.writeDescriptor(d))failed("Cannot subscribe to sensor");
            });}
            public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int result){ui.post(()->{
                if(g!=gatt)return;if(result!=0){failed("Subscription failed ("+result+")");return;}
                cancelTimers();failures=0;Sensor.this.status("Connected • waiting for data");
            });}
            public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){
                byte[] original=c.getValue();final byte[] bytes=original==null?null:original.clone();
                ui.post(()->{if(g!=gatt||!characteristic.equals(c.getUuid()))return;
                    Integer value=Packets.decode(bytes,characteristic.equals(uuid("2a37")));
                    if(value!=null){graph.add(value,SystemClock.elapsedRealtime());Sensor.this.status("Connected • "+device.getName());}
                });
            }
        };
    }
    class Graph extends View {
        final String title,unit; final int accent,minimumMax; final Paint paint=new Paint(3);
        final ArrayList<float[]> points=new ArrayList<>();String status="Disconnected • tap Connect";
        Graph(String t,String u,int color,int max){super(MainActivity.this);title=t;unit=u;accent=color;minimumMax=max;}
        void clear(){points.clear();invalidate();}
        void add(double value,long time){points.add(new float[]{(float)(time/1000.0), (float)value});prune(time/1000.0);invalidate();}
        void prune(double now){while(!points.isEmpty()&&points.get(0)[0]<now-300)points.remove(0);}
        void text(Canvas c,String s,float x,float y,int size,int color){paint.setColor(color);paint.setTextSize(size);c.drawText(s,x,y,paint);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float now=SystemClock.elapsedRealtime()/1000f;prune(now);
            float scale=getWidth()/1000f;
            if(scale<=0)return;
            c.scale(scale,scale);
            float w=1000,h=getHeight()/scale,left=54,top=74,bottom=h-30,right=w-16;
            if(bottom<=top)return;
            text(c,title,10,26,18,accent);
            boolean fresh=!points.isEmpty()&&now-points.get(points.size()-1)[0]<5;
            String value=fresh?String.valueOf(Math.round(points.get(points.size()-1)[1])):"—";
            text(c,value+" "+unit,w-175,40,32,accent);
            text(c,demo?"SIMULATED DATA":(!fresh&&!points.isEmpty()?"No recent data • "+status:status),10,52,13,Color.LTGRAY);
            float max=minimumMax,min=0;
            for(float[] p:points){max=Math.max(max,(float)(Math.ceil(p[1]/100)*100));min=Math.min(min,(float)(Math.floor(p[1]/100)*100));}
            for(int i=0;i<=4;i++){
                float y=top+(bottom-top)*i/4;paint.setColor(Color.rgb(39,53,70));paint.setStrokeWidth(1);c.drawLine(left,y,right,y,paint);
                text(c,""+Math.round(max-(max-min)*i/4),4,y+4,12,Color.LTGRAY);
            }
            for(int i=0;i<=5;i++){float x=left+(right-left)*i/5;text(c,i==5?"now":(5-i)+"m",x-10,h-7,12,Color.LTGRAY);}
            c.save();c.clipRect(left,top,right,bottom);Path path=new Path();float previous=-1;
            for(float[] p:points){float x=right-(now-p[0])/300*(right-left);float y=bottom-(p[1]-min)/(max-min)*(bottom-top);
                if(previous<0||p[0]-previous>5)path.moveTo(x,y);else path.lineTo(x,y);previous=p[0];}
            paint.setColor(accent);paint.setStrokeWidth(3);paint.setStyle(Paint.Style.STROKE);c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);c.restore();
        }
    }
}
