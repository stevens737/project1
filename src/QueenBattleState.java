import java.util.*;

public class QueenBattleState {
    public int width, height;
    public int[][] board; 
    private long currentHash = 0L;
    private static final long[][][] zobristTable = new long[11][11][4];
    
    // Arrays pre-allocated to completely prevent Garbage Collection pauses
    public int[][] dWhite, dBlack;
    public int[] qArr;

    static {
        Random rnd = new Random(42);
        for (int i = 0; i < 11; i++)
            for (int j = 0; j < 11; j++)
                for (int k = 0; k < 4; k++)
                    zobristTable[i][j][k] = rnd.nextLong();
    }

    // Official Constructor
    public QueenBattleState(int w, int h, int[][] whitePos, int[][] blackPos) {
        this.width = w; this.height = h;
        this.board = new int[w + 1][h + 1];
        this.dWhite = new int[w + 1][h + 1];
        this.dBlack = new int[w + 1][h + 1];
        this.qArr = new int[(w * h * 2) + 10]; // Safe queue sizing
        
        for (int[] p : whitePos) { board[p[0]][p[1]] = 1; currentHash ^= zobristTable[p[0]][p[1]][1]; }
        for (int[] p : blackPos) { board[p[0]][p[1]] = 2; currentHash ^= zobristTable[p[0]][p[1]][2]; }
    }

    // Fast Internal Constructor for Cloning
    private QueenBattleState(int w, int h) {
        this.width = w; this.height = h;
        this.board = new int[w + 1][h + 1];
        this.dWhite = new int[w + 1][h + 1];
        this.dBlack = new int[w + 1][h + 1];
        this.qArr = new int[(w * h * 2) + 10];
    }

    // Creates a safe copy of the board for searching
    public QueenBattleState cloneState() {
        QueenBattleState c = new QueenBattleState(this.width, this.height);
        for(int i = 0; i <= width; i++) {
            System.arraycopy(this.board[i], 0, c.board[i], 0, height + 1);
        }
        c.currentHash = this.currentHash;
        return c;
    }

    public void applyMove(int x1, int y1, int x2, int y2, String role) {
        int piece = role.equals("white") ? 1 : 2;
        currentHash ^= zobristTable[x1][y1][piece]; 
        currentHash ^= zobristTable[x1][y1][3]; // Burn source
        currentHash ^= zobristTable[x2][y2][piece];
        board[x1][y1] = -1; board[x2][y2] = piece;
    }

    public void retractMove(int x1, int y1, int x2, int y2, String role) {
        int piece = role.equals("white") ? 1 : 2;
        currentHash ^= zobristTable[x2][y2][piece];
        currentHash ^= zobristTable[x1][y1][3]; 
        currentHash ^= zobristTable[x1][y1][piece];
        board[x1][y1] = piece; board[x2][y2] = 0;
    }

    public int getBurnedCount() {
        int count = 0;
        for (int i = 1; i <= width; i++) {
            for (int j = 1; j <= height; j++) {
                if (board[i][j] == -1) count++;
            }
        }
        return count;
    }

    public List<int[]> getLegalMoves(String role) {
        List<int[]> moves = new ArrayList<>();
        int p = role.equals("white") ? 1 : 2;
        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};
        for (int i = 1; i <= width; i++) {
            for (int j = 1; j <= height; j++) {
                if (board[i][j] == p) {
                    for (int[] d : dirs) {
                        for (int dist = 1; ; dist++) {
                            int nx = i + d[0] * dist, ny = j + d[1] * dist;
                            if (nx >= 1 && nx <= width && ny >= 1 && ny <= height && board[nx][ny] == 0) {
                                moves.add(new int[]{i, j, nx, ny});
                            } else break;
                        }
                    }
                }
            }
        }
        return moves;
    }

    public int evaluate(String role) {
        List<int[]> wMoves = getLegalMoves("white");
        List<int[]> bMoves = getLegalMoves("black");

        if (wMoves.isEmpty() && bMoves.isEmpty()) return 0;
        if (wMoves.isEmpty()) return role.equals("white") ? -10000 : 10000;
        if (bMoves.isEmpty()) return role.equals("white") ? 10000 : -10000;

        int territory = calculateBFSTerritory();
        int score = ((wMoves.size() - bMoves.size()) * 5) + (territory * 20);
        return role.equals("white") ? score : -score;
    }

    private int calculateBFSTerritory() {
        for (int i = 1; i <= width; i++) {
            for (int j = 1; j <= height; j++) {
                dWhite[i][j] = -1;
                dBlack[i][j] = -1;
            }
        }

        bfs(dWhite, 1);
        bfs(dBlack, 2);

        int territory = 0;
        for (int i = 1; i <= width; i++) {
            for (int j = 1; j <= height; j++) {
                if (board[i][j] == 0) {
                    int dw = dWhite[i][j], db = dBlack[i][j];
                    if (dw != -1 && (db == -1 || dw < db)) territory++;
                    else if (db != -1 && (dw == -1 || db < dw)) territory--;
                }
            }
        }
        return territory; // Positive means White is winning space
    }

    private void bfs(int[][] dists, int p) {
        int head = 0, tail = 0;
        for (int i = 1; i <= width; i++) {
            for (int j = 1; j <= height; j++) {
                if (board[i][j] == p) { 
                    dists[i][j] = 0; 
                    qArr[tail++] = i; 
                    qArr[tail++] = j; 
                }
            }
        }
        
        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};
        while (head < tail) {
            int cx = qArr[head++];
            int cy = qArr[head++];
            int d = dists[cx][cy];
            
            for (int[] dir : dirs) {
                for (int dist = 1; ; dist++) {
                    int nx = cx + dir[0] * dist, ny = cy + dir[1] * dist;
                    if (nx >= 1 && nx <= width && ny >= 1 && ny <= height && board[nx][ny] == 0) {
                        if (dists[nx][ny] == -1) {
                            dists[nx][ny] = d + 1;
                            qArr[tail++] = nx;
                            qArr[tail++] = ny;
                        }
                    } else break; // Queen is blocked
                }
            }
        }
    }

    public int quickEvaluateMove(int[] m) {
        int cx = (width + 1) / 2, cy = (height + 1) / 2;
        return 20 - Math.max(Math.abs(m[2] - cx), Math.abs(m[3] - cy));
    }

    public long getHash() { return currentHash; }
}