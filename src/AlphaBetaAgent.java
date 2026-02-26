import java.util.*;
import java.io.*;

public class AlphaBetaAgent implements Agent {
    private String role;
    private int playclock;
    private QueenBattleState state; 
    private long startTime, timeLimit;
    private int nodes;

    // Transposition Table (TT) for caching evaluations
    private static final int TT_SIZE = 1 << 21; 
    private static final int TT_MASK = TT_SIZE - 1;
    
    private long[] ttKeys = new long[TT_SIZE];
    private int[] ttValues = new int[TT_SIZE];
    private int[] ttDepths = new int[TT_SIZE];

    private PrintWriter logWriter;
    private static class TimeoutException extends RuntimeException {}

    // Initializes the agent for a new match
    public void init(String role, int width, int height, int playclock, int[][] whitePos, int[][] blackPos) {
        this.role = role;
        this.playclock = playclock;
        this.state = new QueenBattleState(width, height, whitePos, blackPos);
        
        // Clear the TT for the new match
        Arrays.fill(ttKeys, 0L);
        
        try {
            logWriter = new PrintWriter(new BufferedWriter(new FileWriter("src/agent_log.txt", true)));
            logToFile("\n--- TOURNAMENT MATCH START: " + role + " (" + width + "x" + height + ") ---");
        } catch (IOException e) { }
    }

    // Generates the next action based on the opponent's last move
    public String nextAction(int[] lastMove) {
        if (lastMove != null && lastMove.length >= 4 && lastMove[0] != -1) {
            int sx = lastMove[0], sy = lastMove[1];
            int pieceAtSource = state.board[sx][sy];
            
            if (pieceAtSource == 1) {
                state.applyMove(lastMove[0], lastMove[1], lastMove[2], lastMove[3], "white");
            } else if (pieceAtSource == 2) {
                state.applyMove(lastMove[0], lastMove[1], lastMove[2], lastMove[3], "black");
            }
        }

        int burnedCount = state.getBurnedCount();
        boolean isWhiteTurn = (burnedCount % 2 == 0);
        boolean myTurn = (role.equals("white") && isWhiteTurn) || (role.equals("black") && !isWhiteTurn);

        if (!myTurn) return "noop";

        QueenBattleState searchState = state.cloneState();
        startTime = System.currentTimeMillis();
        // Set time limit with a safety margin to avoid timeouts
        timeLimit = (playclock * 1000) - 700; 
        int[] best = null;
        int depth = 1;
        
        Arrays.fill(ttKeys, 0L); 

        try {
            while (depth < 50) {
                nodes = 0;
                int[] currentBest = startNegamax(searchState, depth);
                if (nodes == 0 && depth > 1) break;
                if (currentBest != null) best = currentBest;
                
                long elapsed = Math.max(1, System.currentTimeMillis() - startTime);
                logToFile("Depth " + depth + " | NPS: " + (nodes * 1000 / elapsed) + " | Nodes: " + nodes);
                depth++;
            }
        } catch (TimeoutException e) {
            logToFile("Timeout caught at Depth " + depth + ". Corrupted search board discarded.");
        }

        if (best != null) {
            state.applyMove(best[0], best[1], best[2], best[3], this.role);
            String moveMsg = "(play " + best[0] + " " + best[1] + " " + best[2] + " " + best[3] + ")";
            logToFile("Playing: " + moveMsg);
            return moveMsg;
        }
        
        return "noop";
    }

    // Clones the current state for search to avoid mutating the original
    private int[] startNegamax(QueenBattleState s, int depth) {
        List<int[]> moves = s.getLegalMoves(role);
        if (moves.isEmpty()) return null;
        
        moves.sort((a, b) -> Integer.compare(s.quickEvaluateMove(b), s.quickEvaluateMove(a)));

        int[] bestMove = moves.get(0);
        int alpha = -30000, beta = 30000, bestV = -30000;

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

    // Negamax search with alpha-beta pruning and TT lookup
    private int negamax(QueenBattleState s, int depth, int alpha, int beta, boolean isMe) {
        nodes++;
        if (nodes % 100 == 0) checkTime();

        int score = s.evaluate(role);
        if (Math.abs(score) >= 10000 || depth <= 0) return isMe ? score : -score;

        long hash = s.getHash();
        int ttIndex = (int)(hash & TT_MASK);
        
        if (ttKeys[ttIndex] == hash && ttDepths[ttIndex] >= depth) {
            return ttValues[ttIndex];
        }

        int bestV = -30000;
        String cur = isMe ? role : (role.equals("white") ? "black" : "white");
        List<int[]> moves = s.getLegalMoves(cur);
        if (moves.isEmpty()) return isMe ? -10000 : 10000;

        // Move ordering using quick heuristic evaluation
        for (int[] m : moves) {
            s.applyMove(m[0], m[1], m[2], m[3], cur);
            int v = -negamax(s, depth - 1, -beta, -alpha, !isMe);
            s.retractMove(m[0], m[1], m[2], m[3], cur);
            bestV = Math.max(v, bestV);
            alpha = Math.max(alpha, bestV);
            if (alpha >= beta) break;
        }

        // Store in TT
        ttKeys[ttIndex] = hash;
        ttValues[ttIndex] = bestV;
        ttDepths[ttIndex] = depth;

        return bestV;
    }

    private void checkTime() { if (System.currentTimeMillis() - startTime >= timeLimit) throw new TimeoutException(); }
    private void logToFile(String msg) { if (logWriter != null) { logWriter.println(msg); logWriter.flush(); } }
    public void cleanup() { if (logWriter != null) logWriter.close(); state = null; }
}