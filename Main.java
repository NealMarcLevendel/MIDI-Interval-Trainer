import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Synthesizer;

public class Main {

    public static BlockingQueue<ArrayList<Set<Integer>>> playbackQueue = new LinkedBlockingQueue<>();

    public static class MidiEvent {
        int channel;
        int note;
        int velocity;
        int duration;

        public MidiEvent(int channel, int note, int velocity, int duration) {
            this.channel = channel;
            this.note = note;
            this.velocity = velocity;
            this.duration = duration;
        }
    }

    public static final int MIN = 21 + 24 + 12; // A0
    public static final int MAX = 108 - 24 - 12; // C8

    public static MidiChannel piano;
    public static MidiChannel drums;

    public static void main(String[] args) throws Exception {

        Synthesizer synth = MidiSystem.getSynthesizer();
        synth.open();
        MidiChannel[] channels = synth.getChannels();
        piano = channels[0];
        drums = channels[9];

        new Thread(() -> {
            while (true) {
                try {
                    ArrayList<Set<Integer>> seq = playbackQueue.take();
                    // (seq, piano, 250);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        MIDIHandler.setupMIDI();

        while (true) { // game loop
        }
    }

    /* Methods to operate synthesizer */

    public static int setPolarity(int note, int interval) {
        if (note + interval > MAX && note - interval >= MIN)
            return -interval;
        else if (note + interval <= MAX && note - interval < MIN)
            return interval;
        else if (note + interval <= MAX && note - interval >= MIN)
            return choose(-1, 1) * interval;
        else
            return 0;
    }

    public static int choose(int a, int b) {
        Random rand = new Random();
        return rand.nextBoolean() ? a : b;
    }

    public static int pick(int[] ints) {
        return ints[random(0, ints.length - 1)];
    }

    public static int random(int min, int max) {
        Random rand = new Random();
        return rand.nextInt(max - min + 1) + min;
    }

}
