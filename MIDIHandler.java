import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.*;
class MIDIHandler {
    static class MIDIEvent {
        public long timeStamp;
        public int note;
        public int velocity;
        public int status;
        public MIDIEvent (int status, int note, int velocity, long timeStamp) {
            this.note = note;
            this.velocity = velocity;
            this.status = status;
            this.timeStamp = timeStamp;
        }
    }
    
    public static int debug = 0;
    
    public static ConcurrentLinkedQueue<MIDIEvent> queue = new ConcurrentLinkedQueue<>();
    public static MidiDevice device;
    public static boolean midiReady = false;

    static List<MIDIEvent> buffer = new ArrayList<>();
    static Set<Integer> attempt = new HashSet<>();
    static List<Integer> question = new ArrayList<>();
    public static boolean inAttempt = true;
    public static long lastEventTime;
    public static final long GAP_MS = 120;
    public static boolean capturing = false;

    public static String[] names = {
    "C","C#","D","D#","E","F",
    "F#","G","G#","A","A#","B"
    };


    public static Set<Integer> HandleMIDI() throws Exception {
        if (!midiReady) {
            throw new IllegalStateException("MIDI not initialized. Call setupMIDI() first.");
        }
        //question.add(60);
        //question.add(65);
        while (true) {
            drainQueue();

            long now = System.currentTimeMillis();

            if (capturing && (now - lastEventTime > GAP_MS)) {
                capturing = false;   // lock the chord
                //debugPrint(attempt.toString());
                Set<Integer> attemptCopy = new HashSet<>(attempt);
                attempt.clear();
                return attemptCopy;
            }
            Thread.sleep(1);
        }
    }
    public static void drainQueue() {
        MIDIEvent e;

        while ((e = queue.poll()) != null) {
            if (e.status == 0x90 && e.velocity > 0) {
                attempt.add(e.note);
                lastEventTime = System.currentTimeMillis();
                if (!capturing) capturing = true;
            } 
        }
    }
    public static void setupMIDI() throws Exception {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        for (MidiDevice.Info info : infos) {
            device = MidiSystem.getMidiDevice(info);

            if (device.getMaxTransmitters() != 0) {
                device.open();

                device.getTransmitter().setReceiver(new Receiver() {
                    @Override
                    public void send(MidiMessage message, long timeStamp) {
                        byte[] m = message.getMessage();

                        if (m.length < 3) return;


                        int status = m[0] & 0xF0;
                        int note = m[1] & 0xFF;
                        int velocity = m[2] & 0xFF;

                        queue.add(new MIDIEvent(status, note, velocity, System.currentTimeMillis()));
                    }

                    @Override
                    public void close() {}
                });
            }
        }
        midiReady = true;
    }
    public static void print(String toPrint) {
        System.out.print("\r\033[2K" + toPrint);
    }
    public static String getName(int midiNote) {
        return names[getNote(midiNote)] + getOctave(midiNote);
    } 
    public static int getNote(int midiNote) {
        return midiNote % 12;
    }
    public static int getOctave(int midiNote) {
        return (midiNote / 12) - 1;
    }
    public static void debugPrint(String str) {
        debug++;
        print(str + " " + debug);
    }
}