package searchclient;

import java.awt.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Heuristic implements Comparator<State> {
    public Heuristic(State initialState) {
        // Here's a chance to pre-process the static parts of the level.
    }

    public int h(State n) {
        HashMap<String, Point> boxMap = new HashMap<>();
        HashMap<String, Point> goalMap = new HashMap<>();

        for (int i = 0; i < State.MAX_ROW ; i++) {
            for (int j = 0; j < State.MAX_COL ; j++) {
                if ('A' <= n.boxes[i][j]  && n.boxes[i][j]  <= 'Z'){
                    boxMap.put(String.valueOf(n.boxes[i][j]).toLowerCase(), new Point(i, j));
                }

                if ('a' <= State.goals[i][j]  && State.goals[i][j]  <= 'z'){
                    goalMap.put(String.valueOf(State.goals[i][j]).toLowerCase(), new Point(i, j));
                }
            }
        }

        AtomicInteger cost = new AtomicInteger(0);

        boxMap.forEach((k,v) -> {
            Point goal = goalMap.get(k);
            int xDistance = Math.abs(goal.x - v.x);
            int yDistance = Math.abs(goal.y - v.y);
            cost.addAndGet(xDistance + yDistance);
        });

        return cost.get();
    }

    public abstract int f(State n);

    @Override
    public int compare(State n1, State n2) {
        return this.f(n1) - this.f(n2);
    }

    public static class AStar extends Heuristic {
        public AStar(State initialState) {
            super(initialState);
        }

        @Override
        public int f(State n) {
            return n.g() + h(n);
        }

        @Override
        public String toString() {
            return "A* evaluation";
        }
    }

    public static class WeightedAStar extends Heuristic {
        private int W;

        public WeightedAStar(State initialState, int W) {
            super(initialState);
            this.W = W;
        }

        @Override
        public int f(State n) {
            return n.g() + this.W * h(n);
        }

        @Override
        public String toString() {
            return String.format("WA*(%d) evaluation", this.W);
        }
    }

    public static class Greedy extends Heuristic {
        public Greedy(State initialState) {
            super(initialState);
        }

        @Override
        public int f(State n) {
            return h(n);
        }

        @Override
        public String toString() {
            return "Greedy evaluation";
        }
    }
}
