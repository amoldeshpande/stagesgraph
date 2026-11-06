import com.stagesgraphs.ZoneProfiles;

public final class ZoneProfilesCheck {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void invalid(String name,int[] boundaries){
        try{new ZoneProfiles.Profile(name,boundaries);throw new AssertionError("Invalid profile accepted");}
        catch(IllegalArgumentException expected){checks++;}
    }
    public static void main(String[] args){
        int[] boundaries={100,120,140,160};
        ZoneProfiles.Profile alex=new ZoneProfiles.Profile(" Alex ",boundaries);
        check(alex.name.equals("Alex"),"Trim username");
        int[] values={0,99,100,119,120,139,140,159,160,220};
        int[] zones={0,0,1,1,2,2,3,3,4,4};
        for(int i=0;i<values.length;i++)check(alex.zone(values[i])==zones[i],"Inclusive boundary at "+values[i]);
        boundaries[0]=999;check(alex.boundary(0)==100,"Profile owns its boundaries");
        invalid(" ",new int[]{100,120,140,160});
        invalid("Rider",new int[]{0,120,140,160});
        invalid("Rider",new int[]{100,100,140,160});
        invalid("Rider",new int[]{120,100,140,160});
        invalid("Rider",new int[]{100,120,140,301});
        invalid("Rider",new int[]{100,120,140});
        ZoneProfiles profiles=new ZoneProfiles();profiles.save(alex);
        profiles.save(new ZoneProfiles.Profile("Zoë := rider",new int[]{110,130,150,170}));
        profiles.select("aLeX");
        ZoneProfiles restored=ZoneProfiles.decode(profiles.encode());
        check(restored.all().size()==2 && restored.selected().name.equals("Alex"),"Restore profiles and active user");
        check(restored.find("zoë := rider").boundary(2)==150,"Unicode and separators survive storage");
        check(restored.find("Alex").boundary(0)==100,"Users keep independent zones");
        restored.save(new ZoneProfiles.Profile("Alex",new int[]{105,125,145,165}));
        check(restored.all().size()==2 && restored.selected().boundary(0)==105,"Editing updates the selected profile");
        check(alex.boundary(0)==100,"Editing cannot change a workout's existing profile snapshot");
        check(ZoneProfiles.decode("count=oops").all().isEmpty(),"Malformed storage is safe");
        ZoneProfiles partiallyCorrupt=ZoneProfiles.decode("count=2\nname.0=Bad\nzones.0=9,2,3,4\nname.1=Good\nzones.1=100,120,140,160\nselected=good\n");
        check(partiallyCorrupt.all().size()==1 && partiallyCorrupt.selected().name.equals("Good"),"Keep valid users when another record is corrupt");
        check(ZoneProfiles.decode(null).selected()==null,"No invented default zones");
        System.out.println(checks+" zone/profile checks passed.");
    }
}
