import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String testCaseName = "";
        int solverChoice = 0;

        try (Scanner scanner = new Scanner(System.in)) {
            System.setOut(new TeePrintStream(baos, originalOut));
            
            System.out.print("Masukkan nama kasus uji (misal: case1): ");
            testCaseName = scanner.nextLine();

            System.out.println("\n--- Pilih Solver ---");
            System.out.println("1: Hybrid Solver (Greedy + CSP)");
            System.out.println("2: Classic Greedy Solver");
            System.out.print("Pilihan Anda (1/2): ");
            solverChoice = scanner.nextInt();
            
            ISolver solver;
            String solverName;

            if (solverChoice == 1) {
                solver = new MinesweeperSolver();
                solverName = "Hybrid Solver (Greedy + CSP)";
            } else {
                solver = new GreedySolver();
                solverName = "Classic Greedy Solver";
            }
            System.out.println("\nMenggunakan solver: " + solverName);

            System.out.println("Membaca file konfigurasi dari: test/" + testCaseName + ".txt");

            new File("test").mkdirs();
            
            int width, height, numBombs, startX, startY;
            try (BufferedReader reader = new BufferedReader(new FileReader("test/" + testCaseName + ".txt"))) {
                String[] line1 = reader.readLine().trim().split(" ");
                width = Integer.parseInt(line1[0]);
                height = Integer.parseInt(line1[1]);

                numBombs = Integer.parseInt(reader.readLine().trim());
                
                String[] line3 = reader.readLine().trim().split(" ");
                startX = Integer.parseInt(line3[0]);
                startY = Integer.parseInt(line3[1]);
                
            } catch (IOException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.err.println("\nError membaca file konfigurasi: " + e.getMessage());
                System.err.println("Pastikan format file 'test/" + testCaseName + ".txt' sudah benar:");
                System.err.println("Baris 1: {lebar} {tinggi}");
                System.err.println("Baris 2: {jumlah_bom}");
                System.err.println("Baris 3: {x_awal} {y_awal}");
                return; 
            }
            
            MinesweeperGame game = new MinesweeperGame(width, height, numBombs, startX, startY, true);
            
            System.out.println("\n--- Papan Awal (Solver: " + solverName + ") ---");
            game.printBoard(0);

            int steps = 1; 
            long startTime = System.nanoTime();

            while (game.getGameState() == MinesweeperGame.GameState.PLAYING) {
                char[][] currentBoard = game.getDisplayBoard();
                
                MinesweeperGame.Move nextMove = solver.findNextMove(currentBoard, numBombs);

                if (nextMove == null) {
                    System.out.println("Solver tidak dapat menemukan langkah selanjutnya. Permainan berhenti.");
                    break;
                }
                
                game.handleMove(nextMove);
                game.printBoard(steps);
                steps++;
            }

            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            
            System.out.println("\n\n--- PERMAINAN SELESAI (Solver: " + solverName + ") ---");
            if (game.getGameState() == MinesweeperGame.GameState.WON) {
                System.out.println("Status: BERHASIL! (Menang)");
            } else {
                System.out.println("Status: GAGAL! (Kalah)");
                game.printBoard(steps);
            }
            System.out.println("Waktu Permainan: " + durationMs + " ms");
            System.out.println("Jumlah Langkah: " + (steps - 1));

        } catch (Exception e) {
             System.err.println("Terjadi error yang tidak terduga: " + e.getMessage());
             e.printStackTrace();
        } finally {
            System.setOut(originalOut);

            if (testCaseName != null && !testCaseName.trim().isEmpty()) {
                try {
                    String subdirectory = (solverChoice == 1) ? "hybrid" : "greedy";
                    File outputDir = new File("result/" + subdirectory);
                    
                    outputDir.mkdirs();
                    
                    String outputPath = outputDir.getPath() + "/" + testCaseName + ".txt";
                    Files.writeString(Paths.get(outputPath), baos.toString());
                    
                    System.out.println("\nOutput log telah disimpan ke: " + outputPath);

                } catch (Exception e) {
                    System.err.println("Gagal menyimpan file output: " + e.getMessage());
                }
            }
        }
    }
}