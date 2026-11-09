package com.stagesgraphs;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;

/** Included only in the emulator preview APK, never in the bike APK. */
public final class EmulatorPreview extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Intent intent=new Intent(getTargetContext(),MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity activity=(MainActivity)startActivitySync(intent);
        runOnMainSync(()->{
            ZoneProfiles.Profile profile=new ZoneProfiles.Profile("Preview",new int[]{100,120,140,160});
            activity.profiles.save(profile);activity.saveProfiles();
            activity.workoutZones=profile;activity.hasWorkout=true;
            long duration=600000L;
            long end=Math.max(SystemClock.elapsedRealtime(),duration+1),begin=end-duration;
            activity.workout.start(begin);
            int[] heart={94,98,104,112,119,126,130,138,145,153,167,160,148};
            for(int i=0;i<=180;i++){
                double position=i*(heart.length-1)/180.0;
                int index=Math.min((int)position,heart.length-2);
                int hr=(int)Math.round(heart[index]+(heart[index+1]-heart[index])*(position-index));
                long time=begin+duration*i/180;
                activity.workout.heart.observe(hr,time);
                activity.workout.power.observe((int)Math.round(220+65*Math.sin(i/15.0)),time);
                activity.workout.sample(time);
            }
            activity.workout.stop(end);activity.refreshWorkout(end);
            TextView banner=new TextView(activity);
            banner.setText("Emulator preview · synthetic readings");banner.setTextColor(Color.LTGRAY);banner.setTextSize(12);
            activity.root.addView(banner,0);
        });
        Bundle status=new Bundle();status.putString("stream","Preview ready\n");sendStatus(0,status);
        // Leave the preview Activity visible for inspection.
    }
}
