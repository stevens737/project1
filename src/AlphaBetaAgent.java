import java.util.*;
import java.io.*;

public class AlphaBetaAgent implements Agent {
    private String role;
    private int playclock;
    private QueenBattleState state;
    private long startTime, timeLimit;
    private int nodes;
    
    private static class Entry { int val, depth; }
    private Map<Long, Entry> tt = new HashMap<>();
    private PrintWriter logWriter;

    private static class TimeoutException extends RuntimeException {}

    public void init(String role, int width, int height, int playclock, int[][] whitePos, int[][] blackPos) {
        this.role = role;
        this.playclock = playclock;
        this.state = new QueenBattleState(width, height, whitePos, blackPos);
        try {
            // FIX: true for append mode, and we flush manually in logToFile
            File logFile = new File("src/agent_log.txt");
            logWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
            logToFile("--- INIT: " + role + " " + new Date().toString() + " ---");
        } catch (IOException e) {
            System.err.println("CRITICAL: Could not open log file in src/agent_log.txt");
        }
    }

    public String nextAction(int[] lastMove) {
        if (lastMove != null && lastMove.length >= 4) {
            String lastMover = role.equals("white") ? "black" : "white";
            state.applyMove(lastMove[0], lastMove[1], lastMove[2], lastMove[3], lastMover);
        }

        startTime = System.currentTimeMillis();
        timeLimit = (playclock * 1000) - 500; 
        int[] best = null;
        int depth = 1;
        tt.clear(); 

        try {
            while (depth < 50) {
                nodes = 0;
                int[] currentBest = startNegamax(state, depth);
                
                // SAFETY EXIT: If no new nodes are found, the tree is solved or fully cached
                if (nodes == 0 && depth > 1) {
                    logToFile("Search complete: No new nodes found at Depth " + depth);
                    break; 
                }
                
                if (currentBest != null) best = currentBest;
                
                long elapsed = Math.max(1, System.currentTimeMillis() - startTime);
                logToFile("Depth " + depth + " | NPS: " + (nodes * 1000 / elapsed) + " | Nodes: " + nodes);
                depth++;
            }
        } catch (TimeoutException e) {
            logToFile("Timeout reached at depth " + (depth - 1));
        }

        if (best != null) {
            state.applyMove(best[0], best[1], best[2], best[3], this.role);
            String action = "(play " + best[0] + " " + best[1] + " " + best[2] + " " + best[3] + ")";
            logToFile("Returning: " + action);
            return action;
        }
        return "noop";
    }

    private int[] startNegamax(QueenBattleState s, int depth) {
        List<int[]> moves = s.getLegalMoves(role);
        if (moves.isEmpty()) return null;
        
        moves.sort((a, b) -> {
            int scoreA = s.quickEvaluateMove(a, role);
            int scoreB = s.quickEvaluateMove(b, role);
            return Integer.compare(scoreB, scoreA);
        });

        int[] bestMove = moves.get(0);
        int alpha = -10000, beta = 10000, bestV = -10000;

        for (int[] m : moves) {
            checkTime();
            s.applyMove(m[0], m[1], m[2], m[3], role);
            int v = -negamax(s, depth - 1, -beta, -alpha, false);
            s.retractMove(m[0], m[1], m[2], m[3], role);
            if (v > bestV) { bestV = v; bestMove = m; }
            alpha = Math.max(alpha, bestV);
        }
        return bestMove;
    }

    private int negamax(QueenBattleState s, int depth, int alpha, int beta, boolean isMe) {
        nodes++;
        if (nodes % 1000 == 0) checkTime();

        int score = s.evaluate(role);
        if (Math.abs(score) == 100 || score == 0) return isMe ? score : -score;
        if (depth <= 0) return isMe ? score : -score;

        long hash = s.getHash();
        Entry e = tt.get(hash);
        if (e != null && e.depth >= depth) return e.val;

        int bestV = -10000;
        String cur = isMe ? role : (role.equals("white") ? "black" : "white");
        List<int[]> moves = s.getLegalMoves(cur);
        
        for (int[] m : moves) {
            s.applyMove(m[0], m[1], m[2], m[3], cur);
            int v = -negamax(s, depth - 1, -beta, -alpha, !isMe);
            s.retractMove(m[0], m[1], m[2], m[3], cur);
            bestV = Math.max(v, bestV);
            alpha = Math.max(alpha, bestV);
            if (alpha >= beta) break;
        }

        Entry newE = new Entry(); newE.val = bestV; newE.depth = depth;
        tt.put(hash, newE);
        return bestV;
    }

    private void checkTime() {
        if (System.currentTimeMillis() - startTime >= timeLimit) throw new TimeoutException();
    }

    private void logToFile(String msg) {
        System.out.println(msg); // Print to console
        if (logWriter != null) {
            logWriter.println(msg); // Print to file
            logWriter.flush();      // FORCE WRITING TO DISK
        }
    }
    
    public void cleanup() {
        logToFile("--- CLEANUP: Game Ended ---");
        if (logWriter != null) logWriter.close();
        tt.clear();
    }
}