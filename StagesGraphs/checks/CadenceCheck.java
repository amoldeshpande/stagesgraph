package com.stagesgraphs;

public final class CadenceCheck {
    private static void check(Integer expected, Integer actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("Expected "+expected+", got "+actual);
        }
    }

    public static void main(String[] args) {
        Packets.Cadence cadence=new Packets.Cadence();
        byte[] first={53,0,0,0,0,0,0,0,0,0,0,0,0,(byte)255,(byte)255,0,(byte)254};
        byte[] next=first.clone();
        next[13]=0;
        next[14]=0;
        next[16]=2;
        check(null,cadence.decode(new byte[4],0));
        check(null,cadence.decode(new byte[]{32,0,0,0},0));
        check(null,cadence.decode(first,0));
        check(60,cadence.decode(next,1000));
        check(60,cadence.decode(next,2000));
        check(0,cadence.decode(next,4000));
        cadence.reset();
        check(null,cadence.decode(next,4100));
        check(null,cadence.decode(first,10000));
        System.out.println("8 cadence packet checks passed.");
    }
}
