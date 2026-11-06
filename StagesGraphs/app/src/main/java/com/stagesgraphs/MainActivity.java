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
    boolean resumed;
    final Workout workout = new Workout();
    TextView elapsed;
    Button startStop, userButton;
    ZoneProfiles profiles;
    ZoneProfiles.Profile workoutZones;
    boolean hasWorkout;
    final int[] zoneColors = {Color.rgb(148,163,184),Color.rgb(96,165,250),Color.rgb(74,222,128),Color.rgb(251,173,80),Color.rgb(251,113,133)};
    final Runnable tick = new Runnable() { public void run() {
        long now = SystemClock.elapsedRealtime();
        workout.sample(now);
        elapsed.setText(Workout.time(workout.duration(now)));
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
        profiles=ZoneProfiles.decode(getSharedPreferences("riders",MODE_PRIVATE).getString("profiles",""));
        LinearLayout actions=new LinearLayout(this); root.addView(actions);
        heart=new Graph("Heart rate","",Color.rgb(255,106,139),220,workout.heart);
        power=new Graph("Power","W",Color.rgb(63,211,236),400,workout.power);
        hr=new Sensor("Heart rate","180d","2a37",heart);
        watts=new Sensor("Power meter","1818","2a63",power);
        userButton=button(actions,profiles.selected()==null?"User":profiles.selected().name,()->chooseUser());
        userButton.setSingleLine(true);userButton.setEllipsize(android.text.TextUtils.TruncateAt.END);
        button(actions,"Heart rate",()->choose(hr));
        button(actions,"Power",()->choose(watts));
        startStop=button(actions,"Start",()->{
            long now=SystemClock.elapsedRealtime();
            if(workout.isRunning()) workout.stop(now); else {workoutZones=profiles.selected();hasWorkout=true;workout.start(now);}
            refreshWorkout(now);
        });
        elapsed=new TextView(this); elapsed.setTextColor(Color.WHITE); elapsed.setTextSize(20);
        elapsed.setGravity(Gravity.CENTER); elapsed.setText("0:00");
        actions.addView(elapsed,new LinearLayout.LayoutParams((int)(120*getResources().getDisplayMetrics().density),-1));
        root.addView(heart,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(power,new LinearLayout.LayoutParams(-1,0,1));
    }
    Button button(LinearLayout row,String label,Runnable action) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(15); row.addView(b,new LinearLayout.LayoutParams(0,-2,1)); b.setOnClickListener(v->action.run());return b;
    }
    void refreshWorkout(long now){startStop.setText(workout.isRunning()?"Stop":"Start");userButton.setEnabled(!workout.isRunning());elapsed.setText(Workout.time(workout.duration(now)));heart.invalidate();power.invalidate();}
    void saveProfiles(){
        getSharedPreferences("riders",MODE_PRIVATE).edit().putString("profiles",profiles.encode()).apply();
        userButton.setText(profiles.selected()==null?"User":profiles.selected().name);
        heart.invalidate();
    }
    void chooseUser(){
        if(workout.isRunning())return;
        final List<ZoneProfiles.Profile> users=profiles.all();
        ArrayList<String> labels=new ArrayList<>();
        for(ZoneProfiles.Profile profile:users)labels.add(profile.name);
        labels.add("Add user");
        if(profiles.selected()!=null)labels.add("Edit zones");
        new AlertDialog.Builder(this).setTitle("User").setItems(labels.toArray(new String[0]),(d,index)->{
            if(index<users.size()){profiles.select(users.get(index).name);saveProfiles();}
            else editUser(index==users.size()?null:profiles.selected());
        }).setNegativeButton("Cancel",null).show();
    }
    void editUser(final ZoneProfiles.Profile existing){
        LinearLayout form=new LinearLayout(this);form.setOrientation(1);
        int padding=(int)(16*getResources().getDisplayMetrics().density);form.setPadding(padding,padding,padding,padding);
        TextView nameLabel=new TextView(this);nameLabel.setText("Username");form.addView(nameLabel);
        EditText name=new EditText(this);name.setSingleLine(true);name.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(32)});
        if(existing!=null){name.setText(existing.name);name.setEnabled(false);}form.addView(name);
        EditText[] boundaries=new EditText[4];
        for(int i=0;i<4;i++){
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
            TextView label=new TextView(this);label.setText("Zone "+(i+2)+" starts");row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            EditText field=new EditText(this);field.setSingleLine(true);field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            field.setContentDescription("Zone "+(i+2)+" starts");
            if(existing!=null)field.setText(String.valueOf(existing.boundary(i)));
            row.addView(field,new LinearLayout.LayoutParams((int)(110*getResources().getDisplayMetrics().density),-2));
            boundaries[i]=field;form.addView(row);
        }
        TextView note=new TextView(this);note.setText("Zone 1 is below the first value.");form.addView(note);
        ScrollView scroll=new ScrollView(this);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?"Add user":"Edit zones").setView(scroll).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{
                if(existing==null&&profiles.find(name.getText().toString())!=null){name.setError("User already exists");return;}
                int[] starts=new int[4];for(int i=0;i<4;i++)starts[i]=Integer.parseInt(boundaries[i].getText().toString());
                profiles.save(new ZoneProfiles.Profile(name.getText().toString(),starts));saveProfiles();dialog.dismiss();
            }catch(NumberFormatException invalid){Toast.makeText(this,"Enter all four zone starts",Toast.LENGTH_SHORT).show();}
            catch(IllegalArgumentException invalid){Toast.makeText(this,invalid.getMessage(),Toast.LENGTH_SHORT).show();}
        }));dialog.show();
    }
    @Override public void onResume(){ super.onResume();resumed=true;ui.post(tick);if(hr.device!=null)hr.connect();if(watts.device!=null)watts.connect(); }
    @Override public void onPause(){ super.onPause();resumed=false;ui.removeCallbacks(tick);stopScan();hr.stop();watts.stop();workout.sample(SystemClock.elapsedRealtime()); }
    BluetoothAdapter.LeScanCallback scanner;
    Runnable scanTimeout;
    void stopScan(){if(scanner!=null&&adapter!=null){adapter.stopLeScan(scanner);scanner=null;}if(scanTimeout!=null)ui.removeCallbacks(scanTimeout);}
    void choose(final Sensor sensor) {
        if(adapter==null||!getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")){sensor.status("Bluetooth unavailable");return;}
        if(!adapter.isEnabled()){startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));return;}
        stopScan();
        final ArrayList<BluetoothDevice> devices=new ArrayList<>();
        final ArrayAdapter<String> rows=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1);
        final AlertDialog dialog=new AlertDialog.Builder(this).setTitle(sensor.label).setAdapter(rows,(d,index)->{
            stopScan();sensor.stop();sensor.device=devices.get(index);sensor.connect();
        }).setNegativeButton("Cancel",null).create();
        dialog.setOnDismissListener(d->stopScan()); dialog.show();
        scanner=(device,rssi,record)->ui.post(()->{
            if(!devices.contains(device)){devices.add(device);rows.add((device.getName()==null?"Unnamed sensor":device.getName())+"\n"+device.getAddress());}
        });
        if(!adapter.startLeScan(scanner)){dialog.setTitle("Scan failed");return;}
        scanTimeout=()->{stopScan();dialog.setTitle(devices.isEmpty()?"No sensors found":sensor.label);};
        ui.postDelayed(scanTimeout,15000);
    }
    class Sensor {
        String label; UUID service,characteristic; Graph graph; BluetoothDevice device; BluetoothGatt gatt;
        boolean active; int failures; Runnable retry,timeout;
        Sensor(String l,String s,String c,Graph v){label=l;service=uuid(s);characteristic=uuid(c);graph=v;}
        void status(String s){graph.status=s;graph.invalidate();}
        void cancelTimers(){if(retry!=null)ui.removeCallbacks(retry);if(timeout!=null)ui.removeCallbacks(timeout);}
        void stop(){active=false;cancelTimers();graph.series.disconnect(SystemClock.elapsedRealtime());if(gatt!=null){BluetoothGatt old=gatt;gatt=null;old.disconnect();old.close();}status("Disconnected");}
        void connect(){
            if(!resumed||device==null||gatt!=null)return;
            active=true;status("Connecting…");
            gatt=device.connectGatt(MainActivity.this,false,callback);
            timeout=()->{if(active)failed("Connection timed out");};ui.postDelayed(timeout,20000);
        }
        void failed(String why){
            android.util.Log.w("StagesGraphs",label+": "+why);
            graph.series.disconnect(SystemClock.elapsedRealtime());
            cancelTimers();if(gatt!=null){BluetoothGatt old=gatt;gatt=null;old.disconnect();old.close();}
            if(active&&resumed){int delay=Math.min(30,2+(failures++*3));status("Retrying in "+delay+"s");retry=()->connect();ui.postDelayed(retry,delay*1000L);}
        }
        final BluetoothGattCallback callback=new BluetoothGattCallback(){
            public void onConnectionStateChange(BluetoothGatt g,int status,int state){ui.post(()->{
                if(g!=gatt)return;
                if(status==BluetoothGatt.GATT_SUCCESS&&state==BluetoothProfile.STATE_CONNECTED){Sensor.this.status("Connecting…");if(!g.discoverServices())failed("Service discovery failed");}
                else if(state==BluetoothProfile.STATE_DISCONNECTED||status!=0)failed("Disconnected ("+status+")");
            });}
            public void onServicesDiscovered(BluetoothGatt g,int result){ui.post(()->{
                if(g!=gatt)return;
                if(result!=0){failed("Service discovery failed");return;}
                BluetoothGattService s=g.getService(service);
                BluetoothGattCharacteristic c=s==null?null:s.getCharacteristic(characteristic);
                if(c==null){stop();Sensor.this.status("Unsupported sensor");return;}
                BluetoothGattDescriptor d=c.getDescriptor(CCC);
                if(d==null||!g.setCharacteristicNotification(c,true)){failed("Cannot enable sensor updates");return;}
                d.setValue((c.getProperties()&BluetoothGattCharacteristic.PROPERTY_NOTIFY)!=0?BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE:BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                if(!g.writeDescriptor(d))failed("Cannot subscribe to sensor");
            });}
            public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int result){ui.post(()->{
                if(g!=gatt)return;if(result!=0){failed("Subscription failed ("+result+")");return;}
                cancelTimers();failures=0;Sensor.this.status("Connected");
            });}
            public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){
                byte[] original=c.getValue();final byte[] bytes=original==null?null:original.clone();
                ui.post(()->{if(g!=gatt||!characteristic.equals(c.getUuid()))return;
                    Integer value=Packets.decode(bytes,characteristic.equals(uuid("2a37")));
                    if(value!=null){graph.series.observe(value,SystemClock.elapsedRealtime());Sensor.this.status("Connected");}
                });
            }
        };
    }
    class Graph extends View {
        final String title,unit; final int accent,minimumMax; final Paint paint=new Paint(3);
        final Workout.Series series;String status="Disconnected";
        Graph(String t,String u,int color,int max,Workout.Series data){super(MainActivity.this);title=t;unit=u;accent=color;minimumMax=max;series=data;}
        String figure(Number number){return number==null?"—":String.valueOf(Math.round(number.doubleValue()))+(unit.isEmpty()?"":" "+unit);}
        void text(Canvas c,String s,float x,float y,int size,int color){paint.setColor(color);paint.setTextSize(size);c.drawText(s,x,y,paint);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);long now=SystemClock.elapsedRealtime();
            float scale=getWidth()/1000f;
            if(scale<=0)return;
            c.scale(scale,scale);
            float w=1000,h=getHeight()/scale,left=54,top=74,bottom=h-30,right=w-16;
            if(bottom<=top)return;
            text(c,title,10,26,18,accent);
            text(c,"Current",620,18,12,Color.LTGRAY);
            text(c,figure(series.current(now)),620,50,30,accent);
            text(c,"Avg",815,18,12,Color.LTGRAY);
            text(c,figure(series.average(now)),815,50,30,accent);
            ZoneProfiles.Profile zones=series==workout.heart?(hasWorkout?workoutZones:profiles.selected()):null;
            Integer current=series.current(now);
            String detail=workout.isRunning()&&current==null&&status.equals("Connected")?"No data":status;
            if(zones!=null&&current!=null)detail+=" · Z"+(zones.zone(current)+1);
            text(c,detail,10,52,13,Color.LTGRAY);
            float max=Math.max(minimumMax,(float)(Math.ceil(series.maximum/100.0)*100));
            float min=Math.min(0,(float)(Math.floor(series.minimum/100.0)*100));
            if(zones!=null){
                min=Math.max(0,zones.boundary(0)-20);
                max=Math.max(zones.boundary(3)+20,series.maximum+5);
                for(Workout.Sample p:series.points)if(p.value!=null)min=Math.min(min,Math.max(0,p.value-5));
                for(int i=0;i<5;i++){
                    float low=i==0?min:zones.boundary(i-1),high=i==4?max:zones.boundary(i);
                    float bandTop=bottom-(Math.min(max,high)-min)/(max-min)*(bottom-top);
                    float bandBottom=bottom-(Math.max(min,low)-min)/(max-min)*(bottom-top);
                    if(bandBottom<=bandTop)continue;
                    int color=zoneColors[i];paint.setColor(Color.argb(60,Color.red(color),Color.green(color),Color.blue(color)));
                    c.drawRect(left,bandTop,right,bandBottom,paint);
                    if(bandBottom-bandTop>=16)text(c,"Z"+(i+1),right-24,(bandTop+bandBottom)/2+4,12,Color.LTGRAY);
                }
            }
            for(int i=0;i<=4;i++){
                float y=top+(bottom-top)*i/4;paint.setColor(Color.rgb(39,53,70));paint.setStrokeWidth(1);c.drawLine(left,y,right,y,paint);
                text(c,""+Math.round(max-(max-min)*i/4),4,y+4,12,Color.LTGRAY);
            }
            long duration=workout.duration(now),axisDuration=Math.max(1000,duration);
            for(int i=0;i<=5;i++){float x=left+(right-left)*i/5;String label=Workout.time(duration*i/5);paint.setTextSize(12);float offset=i==5?paint.measureText(label):i==0?0:paint.measureText(label)/2;text(c,label,x-offset,h-7,12,Color.LTGRAY);}
            c.save();c.clipRect(left,top,right,bottom);Path path=new Path();long previous=-1;float lastX=0,lastY=0;boolean hasPoint=false;
            for(Workout.Sample p:series.points){
                if(p.value==null){previous=-1;continue;}
                float x=left+(float)((double)p.elapsed/axisDuration)*(right-left);float y=bottom-(p.value-min)/(max-min)*(bottom-top);
                if(previous<0||p.elapsed-previous>Workout.FRESH_MS)path.moveTo(x,y);else path.lineTo(x,y);
                previous=p.elapsed;lastX=x;lastY=y;hasPoint=true;
            }
            paint.setColor(accent);paint.setStrokeWidth(3);paint.setStyle(Paint.Style.STROKE);c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);
            if(hasPoint)c.drawCircle(lastX,lastY,3,paint);
            c.restore();
        }
    }
}
