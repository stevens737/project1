import java.util.*;

public class QueenBattleState {
    public int width, height;
    public int[][] board; 
    public List<int[]> whiteQueens, blackQueens;
    private static final long[][][] zobristTable = new long[11][11][4];
    private long currentHash = 0L;

    static {
        Random rnd = new Random(42);
        for (int i = 0; i < 11; i++)
            for (int j = 0; j < 11; j++)
                for (int k = 0; k < 4; k++)
                    zobristTable[i][j][k] = rnd.nextLong();
    }

    public QueenBattleState(int w, int h, int[][] whitePos, int[][] blackPos) {
        this.width = w; this.height = h;
        this.board = new int[w + 1][h + 1];
        this.whiteQueens = new ArrayList<>();
        this.blackQueens = new ArrayList<>();
        for (int[] p : whitePos) {
            board[p[0]][p[1]] = 1; whiteQueens.add(new int[]{p[0], p[1]});
            currentHash ^= zobristTable[p[0]][p[1]][1];
        }
        for (int[] p : blackPos) {
            board[p[0]][p[1]] = 2; blackQueens.add(new int[]{p[0], p[1]});
            currentHash ^= zobristTable[p[0]][p[1]][2];
        }
    }

    public void applyMove(int x1, int y1, int x2, int y2, String role) {
        int piece = role.equals("white") ? 1 : 2;
        currentHash ^= zobristTable[x1][y1][piece]; 
        currentHash ^= zobristTable[x1][y1][3]; // burned square
        currentHash ^= zobristTable[x2][y2][piece];
        board[x1][y1] = -1; board[x2][y2] = piece;
        List<int[]> qs = role.equals("white") ? whiteQueens : blackQueens;
        for (int[] q : qs) { if (q[0] == x1 && q[1] == y1) { q[0] = x2; q[1] = y2; break; } }
    }

    public void retractMove(int x1, int y1, int x2, int y2, String role) {
        int piece = role.equals("white") ? 1 : 2;
        currentHash ^= zobristTable[x2][y2][piece];
        currentHash ^= zobristTable[x1][y1][3];
        currentHash ^= zobristTable[x1][y1][piece];
        board[x1][y1] = piece; board[x2][y2] = 0;
        List<int[]> qs = role.equals("white") ? whiteQueens : blackQueens;
        for (int[] q : qs) { if (q[0] == x2 && q[1] == y2) { q[0] = x1; q[1] = y1; break; } }
    }

    public int quickEvaluateMove(int[] m, String role) {
        int tx = m[2], ty = m[3], count = 0;
        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};
        for (int[] d : dirs) {
            int nx = tx + d[0], ny = ty + d[1];
            if (nx >= 1 && nx <= width && ny >= 1 && ny <= height && board[nx][ny] == 0) count++;
        }
        return count;
    }

    public long getHash() { return currentHash; }

    public List<int[]> getLegalMoves(String role) {
        List<int[]> moves = new ArrayList<>();
        List<int[]> qs = role.equals("white") ? whiteQueens : blackQueens;
        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};
        for (int[] q : qs) {
            for (int[] d : dirs) {
                for (int dist = 1; ; dist++) {
                    int nx = q[0] + d[0] * dist, ny = q[1] + d[1] * dist;
                    if (nx >= 1 && nx <= width && ny >= 1 && ny <= height && board[nx][ny] == 0) moves.add(new int[]{q[0], q[1], nx, ny});
                    else break;
                }
            }
        }
        return moves;
    }

    public int evaluate(String role) {
        int empty = 0;
        for (int i = 1; i <= width; i++)
            for (int j = 1; j <= height; j++)
                if (board[i][j] == 0) empty++;
        if (empty <= width) return 0;
        List<int[]> wM = getLegalMoves("white"), bM = getLegalMoves("black");
        if (wM.isEmpty() && bM.isEmpty()) return 0;
        if (wM.isEmpty()) return role.equals("white") ? -100 : 100;
        if (bM.isEmpty()) return role.equals("white") ? 100 : -100;
        return role.equals("white") ? wM.size() - bM.size() : bM.size() - wM.size();
    }
}