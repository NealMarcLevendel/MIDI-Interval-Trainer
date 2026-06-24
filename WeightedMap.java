import java.util.HashMap;
import java.util.Map;
import java.util.Random;

class WeightedMap<T> {
    public Map<T, Double> weights = new HashMap<>();
    public Random rand = new Random();

    public WeightedMap () {}
    public void put(T item, double weight) {
        weights.put(item, weight);
    }
    public T pick() {
        double total = 0;
        
        for (double w : weights.values()) {
            total += w;
        }

        double r = rand.nextDouble() * total;

        
        for (Map.Entry<T, Double> e : weights.entrySet()) {
            r -= e.getValue();
            if (r <= 0) return e.getKey();
        }

        return null;
    }
    public void clear() {
        weights.clear();
    }
    public int size() {
        return weights.size();
    }
    public double weigh(T item) {
        return weights.get(item);
    }
    public void remove(T item) {
        weights.remove(item);
    }
    public String toString () {
        return weights.toString();
    }
}