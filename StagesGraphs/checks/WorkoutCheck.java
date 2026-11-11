import com.stagesgraphs.Workout;

public final class WorkoutCheck {
    static int checks;
    static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    static boolean near(Double actual, double expected) {
        return actual != null && Math.abs(actual-expected)<0.000001;
    }
    public static void main(String[] args) {
        Workout w=new Workout();
        w.heart.observe(100,0);
        w.sample(0);
        check(w.heart.points.isEmpty() && w.heart.current(0)==null,"Idle does not display/record readings");
        w.start(1000);
        check(w.isRunning() && w.duration(1000)==0,"Start resets the clock");
        w.heart.observe(200,2000);
        w.heart.observe(200,2100);
        w.heart.observe(200,2200);
        w.stop(4000);
        check(near(w.heart.average(4000),500.0/3),"Average is time-weighted, not notification-weighted");
        check(w.heart.current(4000)==200,"Current reading captured on stop");
        int count=w.heart.points.size();
        w.heart.observe(40,5000);
        w.sample(6000);
        check(w.heart.points.size()==count && w.duration(9000)==3000,"Stop freezes the graph and duration");
        check(w.heart.current(9000)==200 && near(w.heart.average(9000),500.0/3),"Stopped figures cannot be overwritten by Bluetooth");
        w.start(6000);
        check(w.heart.points.size()==1 && w.heart.average(6000)==null,"Next Start clears history and average");
        check(w.heart.current(6000)==40 && w.duration(6000)==0,"New workout uses a fresh reading");

        Workout gap=new Workout();
        gap.start(0);gap.power.observe(100,0);gap.sample(0);gap.sample(5000);
        check(gap.power.current(5000)==null && gap.power.points.get(1).value==null,"Expired readings leave graph gaps");
        gap.power.observe(300,10000);gap.stop(12000);
        check(near(gap.power.average(12000),1100.0/7),"Missing sensor time is excluded from the average");
        check(gap.heart.average(12000)==null,"Absent sensors do not contribute zeros");

        Workout disconnected=new Workout();
        disconnected.start(0);disconnected.power.observe(200,0);disconnected.power.disconnect(1000);disconnected.stop(4000);
        check(near(disconnected.power.average(4000),200) && disconnected.power.current(4000)==null,"Disconnect stops counting the previous reading");

        Workout longRide=new Workout();longRide.start(0);
        for(long t=0;t<=7200000;t+=1000){longRide.heart.observe(140,t);longRide.power.observe(250,t);longRide.sample(t);}
        longRide.stop(7200000);
        check(longRide.heart.points.size()==7201 && longRide.heart.points.get(0).elapsed==0,"Two-hour workout retains all history");
        check(near(longRide.power.average(9000000),250),"Long workout average remains correct");
        check(Workout.time(7200000).equals("2:00:00") && Workout.time(61000).equals("1:01"),"Elapsed labels cover hours and minutes");
        Workout peaks=new Workout();
        peaks.start(0);
        check(peaks.power.maximum()==null,"Max stays blank until a reading arrives");
        peaks.power.observe(-20,10);
        peaks.power.observe(-30,20);
        peaks.stop(30);
        peaks.power.observe(999,40);
        check(peaks.power.maximum()==-20,"Max captures readings between graph samples and freezes on Stop");
        peaks.start(6000);
        check(peaks.power.maximum()==null,"Start clears the old maximum");
        Workout paused=new Workout();
        paused.start(0);
        paused.cadence.observe(80,0);
        paused.pause(1000);
        paused.cadence.observe(200,2000);
        paused.sample(2000);
        check(paused.duration(2000)==1000 && paused.cadence.current(2000)==80
                && paused.cadence.maximum()==80,"Pause freezes time, readings and Max");
        paused.cadence.observe(100,4000);
        paused.resume(4000);
        paused.stop(5000);
        check(paused.duration(5000)==2000 && near(paused.cadence.average(5000),90),
                "Resume excludes paused time and readings from the average");
        paused.start(6000);
        paused.pause(7000);
        paused.stop(10000);
        check(!paused.isPaused() && !paused.isRunning() && paused.duration(10000)==1000,
                "Stop can end a paused workout");
        System.out.println(checks+" workout checks passed.");
    }
}
