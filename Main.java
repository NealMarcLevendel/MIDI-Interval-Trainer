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

    public static ArrayList<Set<Integer>> currentSequence = new ArrayList<>();
    public static ArrayList<Integer> generatedIntervals = new ArrayList<>();
    public static WeightedMap<Integer> pool = new WeightedMap<>();

    public static final int MIN = 21+24+12;  // A0
    public static final int MAX = 108-24-12; // C8

    public static MidiChannel channel;

    public static void main(String[] args) throws Exception {


        Synthesizer synth = MidiSystem.getSynthesizer();
        synth.open();
        MidiChannel[] channels = synth.getChannels();
        channel = channels[0];
        new Thread(() -> {
            while (true) {
                try {
                    ArrayList<Set<Integer>> seq = playbackQueue.take();
                    playSequence(seq, channel, 250);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        ).start();

        MIDIHandler.setupMIDI();
        
        while(true) {
        }
    }
    public static void playSequence(ArrayList<Set<Integer>> seq, MidiChannel channel, long time) throws InterruptedException {
        for (Set<Integer> chord : seq) {
            playChord(chord, channel, time);
        }
    }
    public static void playChord (Set<Integer> chord, MidiChannel channel, long time) throws InterruptedException {
        HashSet<Integer> currentlyPlayingNotes = new HashSet<>();
        for (int note : chord) {
            channel.noteOn(note, 100);
            currentlyPlayingNotes.add(note);
        }

        Thread.sleep(time);
        
        for (int note : currentlyPlayingNotes) {
            channel.noteOff(note);
        }
    }
    public static int setPolarity(int note, int interval) {
        if (note + interval > MAX && note - interval >= MIN) return -interval;
        else if (note + interval <= MAX && note - interval < MIN) return interval;
        else if (note + interval <= MAX && note - interval >= MIN) return choose(-1,1) * interval;
        else return 0;
    }
    public static int choose (int a, int b) {
        Random rand = new Random();
        return rand.nextBoolean() ? a : b;
    }
    public static int pick (int [] ints) {
        return ints[random(0, ints.length-1)];
    }
    public static int random(int min, int max) {
        Random rand = new Random();
        return rand.nextInt(max - min + 1) + min;
    }

}
