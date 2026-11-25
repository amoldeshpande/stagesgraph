import com.stagesgraphs.ProfilePassword;

public final class ProfilePasswordCheck {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        String first=ProfilePassword.create("bike"),second=ProfilePassword.create("bike");
        check(ProfilePassword.matches("bike",first),"Correct password accepted");
        check(!ProfilePassword.matches("wrong",first),"Wrong password rejected");
        check(!first.equals(second),"Each password has its own salt");
        check(!first.contains("bike"),"Password not stored as plaintext");
        check(!ProfilePassword.matches("",first),"Empty password rejected");
        check(!ProfilePassword.matches(null,first),"Missing password rejected");
        check(!ProfilePassword.matches("bike",null),"Missing record rejected");
        check(!ProfilePassword.matches("bike","broken"),"Malformed record rejected");
        check(!ProfilePassword.matches("bike",first.replace(':','!')),"Damaged record rejected");
        check(ProfilePassword.matches("bike","000102030405060708090a0b0c0d0e0f:061c01694d452b27b1d87446b86bcd78c90a80aba6bf7b6b405aebe451dc44aa"),"Independent PBKDF2 vector");
        String changed=ProfilePassword.create("new password");
        check(!ProfilePassword.matches("bike",changed)&&ProfilePassword.matches("new password",changed),"Changed password replaces old password");
        try{ProfilePassword.create("");throw new AssertionError("Empty password saved");}catch(IllegalArgumentException expected){checks++;}
        System.out.println(checks+" password checks passed.");
    }
}
