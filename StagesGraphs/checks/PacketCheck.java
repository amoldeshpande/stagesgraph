import com.stagesgraphs.Packets;
public class PacketCheck {
    static void check(Integer expected, Integer actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) throw new AssertionError("Expected "+expected+", got "+actual);
    }
    public static void main(String[] args) {
        check(150, Packets.decode(new byte[]{0,(byte)150},true));
        check(300, Packets.decode(new byte[]{1,44,1},true));
        check(null, Packets.decode(new byte[]{1,44},true));
        check(null, Packets.decode(new byte[]{4,100},true));
        check(100, Packets.decode(new byte[]{6,100},true));
        check(250, Packets.decode(new byte[]{0,0,(byte)250,0},false));
        check(-10, Packets.decode(new byte[]{0,0,(byte)246,(byte)255},false));
        check(null, Packets.decode(new byte[]{0,0,1},false));
        check(null, Packets.decode(null,false));
        System.out.println("9 Bluetooth payload checks passed.");
    }
}
