package searchclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SearchClient {
    public State initialState;
    public State stateHolder;

    public SearchClient(BufferedReader serverMessages) throws Exception {
        // Read lines specifying colors
        String line = serverMessages.readLine();
        if (line.matches("^[a-z]+:\\s*[0-9A-Z](\\s*,\\s*[0-9A-Z])*\\s*$")) {
            System.err.println("Error, client does not support colors.");
            System.exit(1);
        }

        // based on the level you are running, count number of rows and columns

        // e.g. if SAD1.lvl =>

        int max_row = 70;
        int max_column = 70;

        int row = 0;
        boolean agentFound = false;
        this.stateHolder = new State(null, max_row, max_column);

        while (!line.equals("")) {
            for (int col = 0; col < line.length(); col++) {
                char chr = line.charAt(col);

                if (chr == '+') { // Wall.
                    this.stateHolder.walls[row][col] = true;
                } else if ('0' <= chr && chr <= '9') { // Agent.
                    if (agentFound) {
                        System.err.println("Error, not a single agent level");
                        System.exit(1);
                    }
                    agentFound = true;
                    this.stateHolder.agentRow = row;
                    this.stateHolder.agentCol = col;
                } else if ('A' <= chr && chr <= 'Z') { // Box.
                    this.stateHolder.boxes[row][col] = chr;
                } else if ('a' <= chr && chr <= 'z') { // Goal.
                    this.stateHolder.goals[row][col] = chr;
                } else if (chr == ' ') {
                    // Free space.
                } else {
                    System.err.println("Error, read invalid level character: " + (int) chr);
                    System.exit(1);
                }
            }
            line = serverMessages.readLine();
            row++;
        }

        // ___________________T TILFØJELSE___________________________

        // Find max columns
        for (int col = 0; col < this.stateHolder.walls[0].length; col++) {
            if (!this.stateHolder.walls[0][col]) {
                max_column = col;
                break;
            }
        }

        // Find max rows
        for (row = 0; row < this.stateHolder.walls.length; row++) {
            if (!this.stateHolder.walls[row][0]) {
                max_row = row;
                break;
            }
        }

        // Create new initial state
        // The walls and goals are static, so no need to initialize the arrays every
        // time
        State.walls = this.stateHolder.walls;
        State.goals = this.stateHolder.goals;

        this.initialState = new State(null, max_row, max_column);
        this.initialState.boxes = this.stateHolder.boxes;
        this.initialState.agentCol = this.stateHolder.agentCol;
        this.initialState.agentRow = this.stateHolder.agentRow;

        // ____________________T TILFØJELSE ______________________________
    }

    public ArrayList<State> Search(Strategy strategy) {
        System.err.format("Search starting with strategy %s.\n", strategy.toString());
        strategy.addToFrontier(this.initialState);

        int iterations = 0;
        while (true) {
            if (iterations == 1000) {
                System.err.println(strategy.searchStatus());
                iterations = 0;
            }

            if (strategy.frontierIsEmpty()) {
                return null;
            }

            State leafState = strategy.getAndRemoveLeaf();

            if (leafState.isGoalState()) {
                return leafState.extractPlan();
            }

            strategy.addToExplored(leafState);
            for (State n : leafState.getExpandedStates()) { // The list of expanded states is shuffled randomly;
                                                            // see
                // State.java.
                if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {
                    strategy.addToFrontier(n);
                }
            }
            iterations++;
        }
    }

    public static void main(String[] args) throws Exception {
        BufferedReader serverMessages = new BufferedReader(new InputStreamReader(System.in));

        // Use stderr to print to console
        System.err.println("SearchClient initializing. I am sending this using the error output stream.");

        // Read level and create the initial state of the problem
        SearchClient client = new SearchClient(serverMessages);

        Strategy strategy;
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
            case "-bfs":
                strategy = new Strategy.StrategyBFS();
                break;
            case "-dfs":
                strategy = new Strategy.StrategyDFS();
                break;
            case "-astar":
                strategy = new Strategy.StrategyBestFirst(new Heuristic.AStar(client.initialState));
                break;
            case "-wastar":
                strategy = new Strategy.StrategyBestFirst(new Heuristic.WeightedAStar(client.initialState, 5));
                break;
            case "-greedy":
                strategy = new Strategy.StrategyBestFirst(new Heuristic.Greedy(client.initialState));
                break;
            default:
                strategy = new Strategy.StrategyBFS();
                System.err.println(
                        "Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
            }
        } else {
            strategy = new Strategy.StrategyBFS();
            System.err.println(
                    "Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
        }

        ArrayList<State> solution;
        try {
            solution = client.Search(strategy);
        } catch (OutOfMemoryError ex) {
            System.err.println("Maximum memory usage exceeded.");
            solution = null;
        }

        if (solution == null) {
            System.err.println(strategy.searchStatus());
            System.err.println("Unable to solve level.");
            System.exit(0);
        } else {
            System.err.println("\nSummary for " + strategy.toString());
            System.err.println("Found solution of length " + solution.size());
            System.err.println(strategy.searchStatus());

            for (State n : solution) {
                String act = n.action.toString();
                System.out.println(act);
                String response = serverMessages.readLine();
                if (response.contains("false")) {
                    System.err.format("Server responsed with %s to the inapplicable action: %s\n", response, act);
                    System.err.format("%s was attempted in \n%s\n", act, n.toString());
                    break;
                }
            }
        }
    }
}
