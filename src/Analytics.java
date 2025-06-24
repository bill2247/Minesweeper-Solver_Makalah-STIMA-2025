// File: Analytics.java
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Analytics {

    // Record untuk menyimpan hasil dari satu permainan
    private record GameResult(MinesweeperGame.GameState finalState, long durationMs, int steps) {}

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("--- Program Analitik Minesweeper Solver ---");
            
            // 1. Dapatkan Input dari Pengguna
            System.out.print("Masukkan nama kasus uji (dari folder 'test/'): ");
            String testCaseName = scanner.nextLine();
            
            System.out.print("Pilih solver (1: Hybrid, 2: Greedy): ");
            int solverChoice = Integer.parseInt(scanner.nextLine());

            System.out.print("Masukkan jumlah percobaan: ");
            int numTrials = Integer.parseInt(scanner.nextLine());

            System.out.print("Masukkan nama file output untuk analisis (di folder 'analytics/'): ");
            String outputFileName = scanner.nextLine();

            // 2. Baca Konfigurasi Permainan dari File
            int width, height, numBombs, startX, startY;
            try (BufferedReader reader = new BufferedReader(new FileReader("test/" + testCaseName + ".txt"))) {
                String[] line1 = reader.readLine().trim().split(" ");
                width = Integer.parseInt(line1[0]);
                height = Integer.parseInt(line1[1]);
                numBombs = Integer.parseInt(reader.readLine().trim());
                String[] line3 = reader.readLine().trim().split(" ");
                startX = Integer.parseInt(line3[0]);
                startY = Integer.parseInt(line3[1]);
            } catch (Exception e) {
                System.err.println("Gagal membaca file konfigurasi: " + e.getMessage());
                return;
            }

            // 3. Siapkan Solver
            ISolver solver = (solverChoice == 1) ? new MinesweeperSolver() : new GreedySolver();
            String solverName = (solverChoice == 1) ? "Hybrid (Greedy + CSP)" : "Classic Greedy";

            System.out.println("\nMemulai " + numTrials + " percobaan dengan solver: " + solverName);
            
            // 4. Jalankan Simulasi
            List<GameResult> successfulRuns = new ArrayList<>();
            int winCount = 0;
            long totalTimeToWin = 0;
            long totalStepsToWin = 0;

            for (int i = 1; i <= numTrials; i++) {
                System.out.print("\rMenjalankan percobaan: " + i + "/" + numTrials);

                // Jalankan satu permainan dan dapatkan hasilnya
                GameResult result = runSingleGame(width, height, numBombs, startX, startY, solver);
                
                if (result.finalState() == MinesweeperGame.GameState.WON) {
                    winCount++;
                    totalTimeToWin += result.durationMs();
                    totalStepsToWin += result.steps();
                    successfulRuns.add(result);
                }
            }
            System.out.println("\nAnalisis Selesai.");

            // 5. Hitung Statistik dan Siapkan Output
            double winRate = (double) winCount / numTrials * 100;
            double avgTime = (winCount > 0) ? (double) totalTimeToWin / winCount : 0;
            double avgSteps = (winCount > 0) ? (double) totalStepsToWin / winCount : 0;

            StringBuilder report = new StringBuilder();
            report.append("--- Hasil Analisis Kinerja Solver ---\n\n");
            report.append("Konfigurasi      : ").append(testCaseName).append(".txt (").append(width).append("x").append(height).append(", ").append(numBombs).append(" bom)\n");
            report.append("Solver Digunakan : ").append(solverName).append("\n");
            report.append("Jumlah Percobaan : ").append(numTrials).append("\n\n");
            report.append("--- Ringkasan Statistik ---\n");
            report.append(String.format("Jumlah Keberhasilan: %d (%.2f%%)\n", winCount, winRate));
            report.append(String.format("Jumlah Kegagalan   : %d\n", numTrials - winCount));
            report.append(String.format("Rata-rata Waktu    : %.2f ms (untuk percobaan yang berhasil)\n", avgTime));
            report.append(String.format("Rata-rata Langkah  : %.2f langkah (untuk percobaan yang berhasil)\n\n", avgSteps));
            report.append("--- Rincian Percobaan yang Berhasil ---\n");

            for(int i = 0; i < successfulRuns.size(); i++) {
                GameResult result = successfulRuns.get(i);
                report.append(String.format("Percobaan #%d: Waktu = %d ms, Langkah = %d\n", i + 1, result.durationMs(), result.steps()));
            }

            // 6. Tulis Hasil ke File
            try {
                new File("analytics").mkdirs();
                String outputPath = "analytics/" + outputFileName + ".txt";
                Files.writeString(Paths.get(outputPath), report.toString());
                System.out.println("Hasil analisis telah disimpan ke: " + outputPath);
            } catch (IOException e) {
                System.err.println("Gagal menyimpan file analisis: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Terjadi error pada program analisis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Menjalankan satu instance permainan dari awal hingga akhir secara 'silent'.
     * @return GameResult yang berisi status akhir, waktu, dan jumlah langkah.
     */
    private static GameResult runSingleGame(int w, int h, int bombs, int sx, int sy, ISolver solver) {
        // PERUBAHAN: Panggil constructor dengan 'false' untuk menonaktifkan log pembuatan papan
        MinesweeperGame game = new MinesweeperGame(w, h, bombs, sx, sy, false);
        
        int steps = 1;
        long startTime = System.nanoTime();

        while (game.getGameState() == MinesweeperGame.GameState.PLAYING) {
            char[][] currentBoard = game.getDisplayBoard();
            MinesweeperGame.Move nextMove = solver.findNextMove(currentBoard, bombs);
            if (nextMove == null) break;
            game.handleMove(nextMove);
            steps++;
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        return new GameResult(game.getGameState(), durationMs, steps - 1);
    }
}