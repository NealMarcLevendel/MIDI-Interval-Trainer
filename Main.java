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
    public static int sequenceSize = 4;
    public static int sequencePointer = 0;
    public static long expectedTimeForFirstNote = 5000;
    public static long expectedTime = 400;
    public static double weightScale = 0.8;
    
    public static int rootPointer = 0;
    public static List<Integer> rootSequence = List.of(60, 65, 70, 63, 68, 61, 66, 59, 64, 57, 62, 55);

    public static int misses = 0;
    public static int timeouts = 0;

    public static int currentInterval = 1;

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

        long startTime;

        pool.put(2, 128);

        rootPointer = 0;
        generateSequence(rootSequence.get(rootPointer), sequenceSize);
        playbackQueue.add(new ArrayList<>(currentSequence));
        boolean pass = false;
        
        while(true) {
            if (pass) {
                if (rootPointer < rootSequence.size()-1) { //completed circle of fifths
                    rootPointer++;
                } else {
                    System.out.println("Completed circle  timeouts: " + timeouts + " misses: " + misses + " sequence size: " + sequenceSize);
                    if (timeouts < 500 && misses == 500) { // add new intervals
                        sequenceSize++;
                        pool.put(currentInterval, 128.0);
                        currentInterval++;
                    }
                    rootPointer = 0;
                    timeouts = 0;
                    misses = 0;
                    genIntervals();
                }
                generateSequence(rootSequence.get(rootPointer), sequenceSize);

                playbackQueue.add(new ArrayList<>(currentSequence));
                pass = false;
            }

            startTime = System.currentTimeMillis();
            Set<Integer> attempt = MIDIHandler.HandleMIDI();
            long time = System.currentTimeMillis() - startTime;
            
            pass = evaluate(attempt, time);
        }


    }
    public static void genIntervals () {
        List<Integer> intervals = new ArrayList<>();

        for (int i = 0; i < sequenceSize; i++) {
            intervals.add(pool.pick());
        }

        generatedIntervals = new ArrayList<>(intervals);
    }
    public static int genRoot (int root) {
        int next;
        if (root + 5 > MAX && root - 7 >= MIN) next = root - 7;
        else if (root + 5 <= MAX && root - 7 < MIN) next = root + 5;
        else if (root + 5 <= MAX && root - 7 >= MIN) next = root + choose(-7, 5);
        else next = root;

        return next;
    }
    public static void generateSequence (int root, int step) {
        List<Set<Integer>> newSequence = new ArrayList<>();

        int curr = root;
        newSequence.add(Set.of(curr));
        int counter = 0;

        for (int i : generatedIntervals) {
            if (counter == step) return;
            
            curr = curr + i;
            newSequence.add(Set.of(curr));

            counter++;
        }

        currentSequence = new ArrayList<>(newSequence);
    } 
    public static boolean evaluate (Set<Integer> attempt, long time) {
        int interval = getInterval();
        if (currentSequence.get(sequencePointer).equals(attempt)) {
                if (sequencePointer != 0) {
                    if (time < expectedTime) {
                        double weight = pool.weigh(interval);
                        pool.put(interval, weight*weightScale);
                        System.out.println("Hit " + time + " " + expectedTime);
                    } else {
                        timeouts++;
                        System.out.println("timeout " + time + " " + expectedTime);
                    }
                } else {
                    if (time >= expectedTime * (sequenceSize+1.5)) {
                        timeouts++;
                        System.out.println("timeout " + time + " " + (expectedTime * (sequenceSize+2)));
                    } else {
                        System.out.println("Hit " + time + " " + (expectedTime * (sequenceSize+2)));
                    }
                }
            if (sequencePointer < currentSequence.size()-1) {
                sequencePointer++;
                return false;
            } else {
                System.out.println("Success " + pool.toString());
                sequencePointer = 0;
                return true;
            }
        } else {
            System.out.println("Miss " + time + " " + interval);
            if (sequencePointer != 0) {
                double weight = pool.weigh(interval);
                pool.put(interval, weight/weightScale);
            }
            misses++;
            sequencePointer = 0;
            return false;
        }
    }
    public static int getInterval () {
        // if (sequencePointer-1 >= 0) {
        //     //System.out.println("getinterval");
        //     int lowestCurrentNote = Integer.MAX_VALUE;
        //     for (int i : currentSequence.get(sequencePointer)) {
        //         if (i < lowestCurrentNote) lowestCurrentNote = i;
        //     }
        //     int lowestPreviousNote = Integer.MAX_VALUE;
        //     for (int i : currentSequence.get(sequencePointer-1)) {
        //         if (i < lowestPreviousNote) lowestPreviousNote = i;
        //     }
        //     return (lowestCurrentNote-lowestPreviousNote);
        // }
        // return 0;
        if (sequencePointer > 0) {
            return generatedIntervals.get(sequencePointer - 1);
        }
        return 0;
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
