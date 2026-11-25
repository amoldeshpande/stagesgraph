package com.stagesgraphs;

import android.app.Instrumentation;
import android.content.*;
import android.os.*;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.*;

/** Exercises real dialogs on API 22; excluded from the bike APK. */
public final class PasswordUiCheck extends Instrumentation {
    MainActivity activity;
    int checks;
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    AccessibilityNodeInfo find(AccessibilityNodeInfo node,String text,String description){
        if(node==null)return null;
        if(text!=null&&text.contentEquals(node.getText()==null?"":node.getText()))return node;
        if(description!=null&&description.contentEquals(node.getContentDescription()==null?"":node.getContentDescription()))return node;
        for(int i=0;i<node.getChildCount();i++){
            AccessibilityNodeInfo found=find(node.getChild(i),text,description);
            if(found!=null)return found;
        }
        return null;
    }
    AccessibilityNodeInfo shown(String text,String description){
        long deadline=SystemClock.uptimeMillis()+5000;
        do{
            AccessibilityNodeInfo node=find(getUiAutomation().getRootInActiveWindow(),text,description);
            if(node!=null)return node;
            SystemClock.sleep(50);
        }while(SystemClock.uptimeMillis()<deadline);
        throw new AssertionError("Missing control: "+(text==null?description:text));
    }
    void click(String text){
        AccessibilityNodeInfo node=shown(text,null);
        while(!node.isClickable()&&node.getParent()!=null)node=node.getParent();
        if(!node.performAction(AccessibilityNodeInfo.ACTION_CLICK))throw new AssertionError("Cannot click "+text);
        waitForIdleSync();
    }
    void fill(String description,String value){
        Bundle arguments=new Bundle();arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);
        if(!shown(null,description).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,arguments))throw new AssertionError("Cannot fill "+description);
        waitForIdleSync();
    }
    void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    void rejected(String password){
        fill("Password",password);click("Unlock");shown("Unlock",null);
        check(find(getUiAutomation().getRootInActiveWindow(),"Edit zones",null)==null,"Wrong password cannot open zones");
    }
    @Override public void onStart(){
        Intent intent=new Intent(getTargetContext(),MainActivity.class);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity=(MainActivity)startActivitySync(intent);
        String profiles=activity.profiles.encode();
        SharedPreferences passwords=activity.getSharedPreferences("passwords",0);
        Map<String,?> before=new HashMap<>(passwords.getAll());
        Bundle result=new Bundle();int code=-1;
        try{
            ZoneProfiles.Profile legacy=new ZoneProfiles.Profile("Password check",new int[]{100,120,140,160});
            runOnMainSync(()->{activity.profiles.save(legacy);activity.saveProfiles();passwords.edit().remove("password check").apply();activity.editUser(legacy);});
            shown("Set password: Password check",null);
            fill("New password","1234");fill("Confirm password","9999");click("Save");
            check(!passwords.contains("password check"),"Mismatched confirmation cannot create password");
            fill("Confirm password","1234");click("Save");shown("Edit zones",null);
            check(ProfilePassword.matches("1234",passwords.getString("password check",null)),"Legacy profile password saved");
            click("Cancel");
            runOnMainSync(()->activity.editUser(legacy));shown("Unlock",null);rejected("wrong");
            fill("Password","1234");click("Unlock");shown("Edit zones",null);checks++;click("Cancel");
            check(activity.profiles.selected().boundary(0)==100,"Cancel preserves zones");
            click("Heart rate");shown("Unlock",null);rejected("wrong");click("Cancel");checks++;
            click("Power");shown("Unlock",null);click("Cancel");checks++;
            click("Reconnect");
            check(find(getUiAutomation().getRootInActiveWindow(),"Unlock",null)==null,"Reconnect needs no password");
            click("Password check");click("Password");fill("Password","1234");click("Unlock");
            shown("Set password: Password check",null);fill("New password","5678");fill("Confirm password","5678");click("Save");
            check(!ProfilePassword.matches("1234",passwords.getString("password check",null))&&ProfilePassword.matches("5678",passwords.getString("password check",null)),"Password change replaces old password");
            runOnMainSync(()->activity.editUser(null));shown("Add user",null);
            fill("Username","Password check B");fill("Password","4321");fill("Confirm password","4321");
            for(int i=0;i<4;i++)fill("Zone "+(i+2)+" starts",String.valueOf(110+i*20));
            click("Save");shown("Password check B",null);
            check(ProfilePassword.matches("4321",passwords.getString("password check b",null)),"New user password saved");
            runOnMainSync(()->activity.editUser(activity.profiles.selected()));shown("Unlock",null);rejected("5678");
            fill("Password","4321");click("Unlock");shown("Edit zones",null);checks++;click("Cancel");
            result.putString("stream",checks+" password UI checks passed\n");
        }catch(Throwable failure){code=0;result.putString("stream","FAILED: "+failure+"\n");}
        finally{
            runOnMainSync(()->{
                SharedPreferences.Editor editor=passwords.edit().clear();
                for(Map.Entry<String,?> entry:before.entrySet())editor.putString(entry.getKey(),(String)entry.getValue());
                editor.commit();activity.profiles=ZoneProfiles.decode(profiles);activity.saveProfiles();
            });
        }
        finish(code,result);
    }
}
