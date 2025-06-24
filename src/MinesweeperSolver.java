import java.util.*;
import java.util.stream.Collectors;

public class MinesweeperSolver implements ISolver {

    private record Coordinate(int row, int col) {}
    private record InternalMove(MinesweeperGame.MoveType action, Coordinate coordinate, double probability) {}

    private int rows;
    private int cols;
    private char[][] board;
    private int totalBombs;

    /**
     * Metode utama yang menjadi "otak" dari solver.
     * Menerima kondisi papan dan total bom, lalu mengembalikan satu langkah terbaik.
     * Alur kerjanya adalah: Coba Greedy -> Jika gagal, coba CSP -> Jika gagal, tebak acak.
     * @param currentBoard Papan permainan saat ini dalam bentuk matriks char.
     * @param totalBombs Jumlah total bom yang ada di papan.
     * @return Objek Move yang merepresentasikan aksi terbaik untuk diambil.
     */
    @Override
    public MinesweeperGame.Move findNextMove(char[][] currentBoard, int totalBombs) {
        this.board = currentBoard;
        this.rows = board.length;
        this.cols = board[0].length;
        this.totalBombs = totalBombs;

        List<InternalMove> certainMoves = findDeterministicMoves();
        if (!certainMoves.isEmpty()) {
            InternalMove move = certainMoves.get(0);
            System.out.println("Langkah Pasti Ditemukan oleh Modul Greedy: " + move.action() + " di [" + move.coordinate().row + ", " + move.coordinate().col + "]");
            return new MinesweeperGame.Move(move.action(), move.coordinate().col, move.coordinate().row);
        }

        System.out.println("Tidak ada langkah pasti. Menjalankan Modul Analisis CSP...");
        int knownFlags = countFlags();
        int remainingBombs = this.totalBombs - knownFlags;

        List<InternalMove> probabilisticMoves = analyzeLocalPatternsAsCSP(remainingBombs);

        if (probabilisticMoves.isEmpty()) {
            System.out.println("Analisis CSP tidak menemukan solusi. Melakukan tebakan acak sebagai fallback...");
            return findRandomMove(); 
        }

        probabilisticMoves.sort(Comparator.comparingDouble(InternalMove::probability));

        InternalMove bestInternalMove = probabilisticMoves.get(0);
        MinesweeperGame.MoveType finalAction = MinesweeperGame.MoveType.DIG;

        if (bestInternalMove.probability() == 0.0) {
            finalAction = MinesweeperGame.MoveType.DIG;
             System.out.println("Hasil CSP menemukan langkah 100% AMAN.");
        } else if (bestInternalMove.probability() == 1.0) {
            finalAction = MinesweeperGame.MoveType.FLAG;
             System.out.println("Hasil CSP menemukan langkah 100% BOM.");
        }

        System.out.printf("Langkah terbaik berdasarkan probabilitas: %s di [%d, %d] (Prob Bom: %.2f%%)%n",
                finalAction, bestInternalMove.coordinate().row, bestInternalMove.coordinate().col, bestInternalMove.probability() * 100);

        return new MinesweeperGame.Move(finalAction, bestInternalMove.coordinate().col, bestInternalMove.coordinate().row);
    }
        
    /**
     * Modul Greedy: Mencari semua langkah yang 100% pasti.
     * Fungsi ini mengimplementasikan dua aturan dasar Minesweeper dan memprioritaskan
     * aksi "chording" (buka sekitar) untuk efisiensi jumlah langkah.
     * @return Sebuah list berisi langkah-langkah pasti. Kosong jika tidak ada.
     */
    private List<InternalMove> findDeterministicMoves() { 
        List<InternalMove> chordingMoves = new ArrayList<>();
        List<InternalMove> flaggingMoves = new ArrayList<>();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (Character.isDigit(board[r][c])) {
                    int numValue = Character.getNumericValue(board[r][c]);
                    List<Coordinate> neighbors = getNeighbors(r, c);
                    
                    long flagCount = neighbors.stream()
                        .filter(n -> board[n.row()][n.col()] == 'F').count();
                    List<Coordinate> coveredNeighbors = neighbors.stream()
                        .filter(n -> board[n.row()][n.col()] == 'C')
                        .collect(Collectors.toList());
                    if (numValue == flagCount && !coveredNeighbors.isEmpty()) {
                      
                        Coordinate chordCoord = new Coordinate(r, c);
                        chordingMoves.add(new InternalMove(MinesweeperGame.MoveType.DIG, chordCoord, 0.0));
                    }

                    if (numValue == flagCount + coveredNeighbors.size() && !coveredNeighbors.isEmpty()) {
                        
                        coveredNeighbors.forEach(n -> flaggingMoves.add(new InternalMove(MinesweeperGame.MoveType.FLAG, n, 1.0)));
                    }
                }
            }
        }

        if (!chordingMoves.isEmpty()) {
            System.out.println("Solver: Peluang 'Buka Sekitar' (Chording) terdeteksi.");
            return chordingMoves;
        }

        if (!flaggingMoves.isEmpty()) {
            System.out.println("Solver: Langkah penandaan (FLAG) terdeteksi.");
            return flaggingMoves;
        }
        
        return Collections.emptyList();
    }

    /**
     * Modul CSP: Menganalisis pola lokal untuk menghitung probabilitas bom yang akurat.
     * Ini adalah "otak" kedua solver yang bekerja saat langkah pasti tidak ditemukan.
     * @param remainingBombs Jumlah bom yang tersisa di papan.
     * @return Sebuah list berisi langkah-langkah probabilistik.
     */
    private List<InternalMove> analyzeLocalPatternsAsCSP(int remainingBombs) {
        Set<Coordinate> frontier = new HashSet<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c] == 'C') {
                    boolean isFrontier = getNeighbors(r, c).stream()
                            .anyMatch(n -> Character.isDigit(board[n.row()][n.col()]));
                    if (isFrontier) {
                        frontier.add(new Coordinate(r, c));
                    }
                }
            }
        }

        if (frontier.isEmpty()) return Collections.emptyList();

        List<Coordinate> variables = new ArrayList<>(findConnectedFrontier(frontier));
        if (variables.isEmpty()) return Collections.emptyList();
  
        Map<String, Integer> memo = new HashMap<>(); 
        Map<Coordinate, List<boolean[]>> solutionStore = new HashMap<>(); 

        
        int totalSolutions = solve(variables, remainingBombs, 0, 0, solutionStore, memo);

        if (totalSolutions == 0) return Collections.emptyList();

        Map<Coordinate, Integer> bombCounts = new HashMap<>();
        List<boolean[]> allConfigs = solutionStore.values().iterator().next();

        for (int i = 0; i < variables.size(); i++) {
            int count = 0;
            for (boolean[] config : allConfigs) {
                if (config[i]) { 
                    count++;
                }
            }
            bombCounts.put(variables.get(i), count);
        }

        return variables.stream()
                .map(v -> {
                    double probability = (double) bombCounts.getOrDefault(v, 0) / totalSolutions;
                    return new InternalMove(MinesweeperGame.MoveType.DIG, v, probability);
                })
                .collect(Collectors.toList());
    }

    /**
     * Mesin Backtracking Rekursif DENGAN MEMOIZATION (Program Dinamis Top-Down).
     * Fungsi ini mengembalikan jumlah total konfigurasi valid dari state saat ini.
     */
    private int solve(List<Coordinate> variables, int remainingBombs, int index, int bombsPlaced,
                    Map<Coordinate, List<boolean[]>> solutionStore, Map<String, Integer> memo) {
        
        String stateKey = index + "," + bombsPlaced;
        if (memo.containsKey(stateKey)) {
            
            return memo.get(stateKey);
        }

        if (!isPartialConfigurationValid(variables, index)) {
            return 0; 
        }
        if (bombsPlaced > remainingBombs) {
            return 0; 
        }

        if (index == variables.size()) {
            if (bombsPlaced == remainingBombs && isFinalConfigurationValid(variables)) {
                
                boolean[] currentConfig = new boolean[variables.size()];
                for (int i = 0; i < variables.size(); i++) {
                    
                    if (board[variables.get(i).row()][variables.get(i).col()] == 'B') {
                        currentConfig[i] = true;
                    }
                }
                solutionStore.computeIfAbsent(variables.get(0), k -> new ArrayList<>()).add(currentConfig);
                return 1;
            }
            return 0; 
        }

        Coordinate currentVar = variables.get(index);
        int totalSolutions = 0;

        board[currentVar.row()][currentVar.col()] = 'B';
        totalSolutions += solve(variables, remainingBombs, index + 1, bombsPlaced + 1, solutionStore, memo);

        board[currentVar.row()][currentVar.col()] = '_';
        totalSolutions += solve(variables, remainingBombs, index + 1, bombsPlaced, solutionStore, memo);

        board[currentVar.row()][currentVar.col()] = 'C';

        memo.put(stateKey, totalSolutions);
        return totalSolutions;
    }

    /**
     * Helper untuk pruning: Mengecek apakah konfigurasi parsial (sejauh `assignedCount`)
     * sudah melanggar aturan angka petunjuk.
     * @return `false` jika konfigurasi sudah pasti salah, `true` jika masih mungkin benar.
     */
    private boolean isPartialConfigurationValid(List<Coordinate> variables, int assignedCount) {
        Set<Coordinate> relevantNumbers = new HashSet<>();
        for(int i=0; i < assignedCount; i++) {
            Coordinate var = variables.get(i);
            for(Coordinate neighbor : getNeighbors(var.row, var.col)) {
                if(Character.isDigit(board[neighbor.row][neighbor.col])) {
                    relevantNumbers.add(neighbor);
                }
            }
        }

        for (Coordinate numCoord : relevantNumbers) {
            int numValue = Character.getNumericValue(board[numCoord.row][numCoord.col]);
            List<Coordinate> neighbors = getNeighbors(numCoord.row, numCoord.col);
            long surroundingBombs = neighbors.stream().filter(n -> board[n.row()][n.col()] == 'F' || board[n.row()][n.col()] == 'B').count();
            long surroundingUnknown = neighbors.stream().filter(n -> board[n.row()][n.col()] == 'C').count();
            
            if (surroundingBombs > numValue || surroundingBombs + surroundingUnknown < numValue) {
                return false;
            }
        }
        return true;
    }
        
    /**
     * Helper untuk validasi akhir: Mengecek apakah konfigurasi final (semua variabel di-assign)
     * konsisten dengan semua angka petunjuk di sekitarnya.
     * @return `true` jika seluruh konfigurasi valid.
     */
    private boolean isFinalConfigurationValid(List<Coordinate> variables) {
        Set<Coordinate> relevantNumbers = new HashSet<>();
        for(Coordinate var : variables) {
            relevantNumbers.addAll(getNeighbors(var.row, var.col));
        }

        for (Coordinate numCoord : relevantNumbers) {
            if (Character.isDigit(board[numCoord.row][numCoord.col])) {
                int numValue = Character.getNumericValue(board[numCoord.row][numCoord.col]);
                long surroundingBombs = getNeighbors(numCoord.row, numCoord.col).stream()
                        .filter(n -> board[n.row()][n.col()] == 'F' || board[n.row()][n.col()] == 'B')
                        .count();
                if (surroundingBombs != numValue) return false;
            }
        }
        return true;
    }

    /**
     * Helper untuk mendapatkan semua 8 tetangga yang valid dari sebuah sel.
     */
    private List<Coordinate> getNeighbors(int r, int c) {
        List<Coordinate> neighbors = new ArrayList<>();
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr;
                int nc = c + dc;
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                    neighbors.add(new Coordinate(nr, nc));
                }
            }
        }
        return neighbors;
    }

    /**
     * Helper untuk menghitung jumlah bendera ('F') yang sudah ada di papan.
     */
    private int countFlags() {
        int count = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c] == 'F') count++;
            }
        }
        return count;
    }
        
    /**
     * Helper untuk menemukan satu grup sel "frontier" yang saling terhubung
     * menggunakan algoritma Breadth-First Search (BFS).
     * Juga membatasi ukuran grup agar tidak terlalu besar (>16) untuk menjaga performa.
     */
    private List<Coordinate> findConnectedFrontier(Set<Coordinate> frontier) {
        if (frontier.isEmpty()) return new ArrayList<>();
        List<Coordinate> connectedComponent = new ArrayList<>();
        Queue<Coordinate> queue = new LinkedList<>();
        
        Coordinate startNode = frontier.iterator().next();
        queue.add(startNode);
        Set<Coordinate> visited = new HashSet<>();
        visited.add(startNode);

        while(!queue.isEmpty()){
            Coordinate current = queue.poll();
            connectedComponent.add(current);
            getNeighbors(current.row, current.col).stream()
                    .filter(frontier::contains)
                    .filter(n -> !visited.contains(n))
                    .forEach(n -> {
                        visited.add(n);
                        queue.add(n);
                    });
        }
        return connectedComponent.size() > 16 ? new ArrayList<>() : connectedComponent;
    }

    /**
     * Helper untuk fallback terakhir: Memilih langkah acak jika tidak ada
     * metode lain yang bisa memberikan keputusan.
     */
    private MinesweeperGame.Move findRandomMove() {
        List<Coordinate> coveredCells = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c] == 'C') {
                    coveredCells.add(new Coordinate(r, c));
                }
            }
        }
        if (!coveredCells.isEmpty()) {
            Coordinate randomCell = coveredCells.get(new Random().nextInt(coveredCells.size()));
            System.out.println("MinesweeperSolver: Memilih sel acak di [" + randomCell.row + ", " + randomCell.col + "]");
            return new MinesweeperGame.Move(MinesweeperGame.MoveType.DIG, randomCell.col, randomCell.row);
        }
        return null; 
    }

}