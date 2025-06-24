import java.util.*;
import java.util.stream.Collectors;

public class MinesweeperSolver implements ISolver {

    private record Coordinate(int row, int col) {}
    private record InternalMove(MinesweeperGame.MoveType action, Coordinate coordinate, double probability) {}

    private int rows, cols, totalBombs;
    private char[][] board;

    @Override
    public MinesweeperGame.Move findNextMove(char[][] currentBoard, int totalBombs) {
        this.board = currentBoard;
        this.rows = board.length;
        this.cols = board[0].length;
        this.totalBombs = totalBombs;

        List<InternalMove> certainMoves = findDeterministicMoves_NoChording();
        if (!certainMoves.isEmpty()) {
            InternalMove move = certainMoves.get(0);
            return new MinesweeperGame.Move(move.action(), move.coordinate().col, move.coordinate().row);
        }

        int remainingBombs = this.totalBombs - countFlags();
        List<InternalMove> probabilisticMoves = analyzeLocalPatternsAsCSP(remainingBombs);
        
        if (probabilisticMoves.isEmpty()) {
            System.out.println("Analisis CSP tidak menemukan solusi. Melakukan tebakan cerdas sebagai fallback...");
            return findBestGuessMove(); 
        }
        
        probabilisticMoves.sort(Comparator.comparingDouble(InternalMove::probability));
        InternalMove bestMove = probabilisticMoves.get(0);
        
        MinesweeperGame.MoveType finalAction = (bestMove.probability() < 0.99) ? 
                                               MinesweeperGame.MoveType.DIG : 
                                               MinesweeperGame.MoveType.FLAG;

        return new MinesweeperGame.Move(finalAction, bestMove.coordinate().col, bestMove.coordinate().row);
    }
    
    private List<InternalMove> findDeterministicMoves_NoChording() { 
        List<InternalMove> moves = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (Character.isDigit(board[r][c])) {
                    int numValue = Character.getNumericValue(board[r][c]);
                    List<Coordinate> neighbors = getNeighbors(r, c);
                    long flagCount = neighbors.stream().filter(n -> board[n.row()][n.col()] == 'F').count();
                    List<Coordinate> coveredNeighbors = neighbors.stream().filter(n -> board[n.row()][n.col()] == 'C').collect(Collectors.toList());

                    if (!coveredNeighbors.isEmpty()) {
                        
                        if (numValue == flagCount) {
                            coveredNeighbors.forEach(n -> moves.add(new InternalMove(MinesweeperGame.MoveType.DIG, n, 0.0)));
                        }
                        
                        if (numValue == flagCount + coveredNeighbors.size()) {
                            coveredNeighbors.forEach(n -> moves.add(new InternalMove(MinesweeperGame.MoveType.FLAG, n, 1.0)));
                        }
                    }
                }
            }
        }
        return moves;
    }

    private List<InternalMove> analyzeLocalPatternsAsCSP(int remainingBombs) {
        Set<Coordinate> frontier = findFrontierCellsAsSet();
        if (frontier.isEmpty()) return Collections.emptyList();
        List<Coordinate> variables = findConnectedFrontier(frontier);
        if (variables.isEmpty()) return Collections.emptyList();

        Map<String, Long> memo = new HashMap<>();
        List<long[]> bombTallies = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) bombTallies.add(new long[]{0});
        long totalSolutions = solve(variables, remainingBombs, 0, 0, bombTallies, memo);
        if (totalSolutions == 0) return Collections.emptyList();
        
        return variables.stream().map(v -> {
            int index = variables.indexOf(v);
            double probability = (double) bombTallies.get(index)[0] / totalSolutions;
            return new InternalMove(MinesweeperGame.MoveType.DIG, v, probability);
        }).collect(Collectors.toList());
    }

    private long solve(List<Coordinate> variables, int remainingBombs, int index, int bombsPlaced,
                     List<long[]> bombTallies, Map<String, Long> memo) {
        String stateKey = index + "," + bombsPlaced;
        if (memo.containsKey(stateKey)) {
            return memo.get(stateKey);
        }
        if (bombsPlaced > remainingBombs || bombsPlaced + (variables.size() - index) < remainingBombs) {
            return 0;
        }
        if (!isPartialConfigurationValid(variables, index)) {
            return 0;
        }
        if (index == variables.size()) {
            if (bombsPlaced == remainingBombs && isFinalConfigurationValid(variables)) {
                for (int i = 0; i < variables.size(); i++) {
                    if (board[variables.get(i).row()][variables.get(i).col()] == 'B') {
                        bombTallies.get(i)[0]++;
                    }
                }
                return 1;
            }
            return 0;
        }
        Coordinate currentVar = variables.get(index);
        long totalSolutions = 0;
        board[currentVar.row()][currentVar.col()] = 'B';
        totalSolutions += solve(variables, remainingBombs, index + 1, bombsPlaced + 1, bombTallies, memo);
        board[currentVar.row()][currentVar.col()] = '_';
        totalSolutions += solve(variables, remainingBombs, index + 1, bombsPlaced, bombTallies, memo);
        board[currentVar.row()][currentVar.col()] = 'C';
        memo.put(stateKey, totalSolutions);
        return totalSolutions;
    }

    private MinesweeperGame.Move findBestGuessMove() {
        List<Coordinate> frontierCells = findFrontierCellsAsList();

        if (frontierCells.isEmpty()) {
            System.out.println("Tidak ada frontier, melakukan tebakan acak murni sebagai fallback terakhir.");
            return findRandomMove();
        }

        Coordinate bestCellToDig = null;
        double lowestRiskScore = Double.MAX_VALUE;

        for (Coordinate candidate : frontierCells) {
            double currentRiskScore = 0.0;
            List<Coordinate> numberNeighbors = getNeighbors(candidate.row, candidate.col).stream()
                    .filter(n -> Character.isDigit(board[n.row()][n.col()]))
                    .collect(Collectors.toList());

            for (Coordinate numNeighbor : numberNeighbors) {
                int numValue = Character.getNumericValue(board[numNeighbor.row][numNeighbor.col]);
                List<Coordinate> neighborsOfNumber = getNeighbors(numNeighbor.row, numNeighbor.col);
                long flagCount = neighborsOfNumber.stream().filter(n -> board[n.row()][n.col()] == 'F').count();
                long coveredCount = neighborsOfNumber.stream().filter(n -> board[n.row()][n.col()] == 'C').count();
                if (coveredCount > 0) {
                    currentRiskScore += (double)(numValue - flagCount) / coveredCount;
                }
            }
            if (currentRiskScore < lowestRiskScore) {
                lowestRiskScore = currentRiskScore;
                bestCellToDig = candidate;
            }
        }
        
        if (bestCellToDig == null) {
            return findRandomMove();
        }

        System.out.printf("Tebakan Cerdas: Memilih sel [%d, %d] dengan skor risiko terendah (%.2f)%n",
                bestCellToDig.row, bestCellToDig.col, lowestRiskScore);
        return new MinesweeperGame.Move(MinesweeperGame.MoveType.DIG, bestCellToDig.col, bestCellToDig.row);
    }
    
    private boolean isPartialConfigurationValid(List<Coordinate> variables, int assignedCount) {
        Set<Coordinate> relevantNumbers = new HashSet<>();
        for(int i = 0; i < assignedCount; i++) {
            getNeighbors(variables.get(i).row, variables.get(i).col).stream()
                .filter(n -> Character.isDigit(board[n.row][n.col()]))
                .forEach(relevantNumbers::add);
        }
        for (Coordinate numCoord : relevantNumbers) {
            int numValue = Character.getNumericValue(board[numCoord.row][numCoord.col]);
            List<Coordinate> neighbors = getNeighbors(numCoord.row, numCoord.col);
            long surroundingBombs = neighbors.stream().filter(n -> board[n.row()][n.col()] == 'F' || board[n.row()][n.col()] == 'B').count();
            if (surroundingBombs > numValue) return false;
            long surroundingUnknown = neighbors.stream().filter(n -> board[n.row()][n.col()] == 'C' || board[n.row()][n.col()] == '_').count();
            if (surroundingBombs + surroundingUnknown < numValue) return false;
        }
        return true;
    }
    
    private boolean isFinalConfigurationValid(List<Coordinate> variables) {
        Set<Coordinate> relevantNumbers = new HashSet<>();
        for(Coordinate var : variables) {
            getNeighbors(var.row, var.col).stream()
                .filter(n -> Character.isDigit(board[n.row()][n.col()]))
                .forEach(relevantNumbers::add);
        }
        for (Coordinate numCoord : relevantNumbers) {
            int numValue = Character.getNumericValue(board[numCoord.row][numCoord.col]);
            long surroundingBombs = getNeighbors(numCoord.row, numCoord.col).stream()
                    .filter(n -> board[n.row()][n.col()] == 'F' || board[n.row()][n.col()] == 'B')
                    .count();
            if (surroundingBombs != numValue) return false;
        }
        return true;
    }
    
    private List<Coordinate> findFrontierCellsAsList() {
        List<Coordinate> frontier = new ArrayList<>();
        for (int r = 0; r < rows; r++) { for (int c = 0; c < cols; c++) { if (board[r][c] == 'C' && getNeighbors(r, c).stream().anyMatch(n -> Character.isDigit(board[n.row()][n.col()]))) { frontier.add(new Coordinate(r, c)); } } }
        return frontier;
    }

    private Set<Coordinate> findFrontierCellsAsSet() {
        Set<Coordinate> frontier = new HashSet<>();
        for (int r = 0; r < rows; r++) { for (int c = 0; c < cols; c++) { if (board[r][c] == 'C' && getNeighbors(r, c).stream().anyMatch(n -> Character.isDigit(board[n.row()][n.col()]))) { frontier.add(new Coordinate(r, c)); } } }
        return frontier;
    }

    private MinesweeperGame.Move findRandomMove() {
        List<Coordinate> coveredCells = new ArrayList<>();
        for (int r = 0; r < rows; r++) { for (int c = 0; c < cols; c++) { if (board[r][c] == 'C') { coveredCells.add(new Coordinate(r, c)); } } }
        if (!coveredCells.isEmpty()) {
            Coordinate randomCell = coveredCells.get(new Random().nextInt(coveredCells.size()));
            return new MinesweeperGame.Move(MinesweeperGame.MoveType.DIG, randomCell.col, randomCell.row);
        }
        return null; 
    }

    private List<Coordinate> getNeighbors(int r, int c) {
        List<Coordinate> neighbors = new ArrayList<>();
        for (int dr = -1; dr <= 1; dr++) { for (int dc = -1; dc <= 1; dc++) { if (dr == 0 && dc == 0) continue; int nr = r + dr; int nc = c + dc; if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) { neighbors.add(new Coordinate(nr, nc)); } } }
        return neighbors;
    }

    private int countFlags() {
        int count = 0;
        for (char[] row : board) for (char cell : row) if (cell == 'F') count++;
        return count;
    }
    
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
            getNeighbors(current.row, current.col).stream().filter(frontier::contains).filter(n -> !visited.contains(n)).forEach(n -> { visited.add(n); queue.add(n); });
        }
        return connectedComponent.size() > 16 ? new ArrayList<>() : connectedComponent;
    }
}