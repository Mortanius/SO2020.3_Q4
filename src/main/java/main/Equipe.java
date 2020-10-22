package main;

import java.util.Random;
import java.util.concurrent.Semaphore;

public class Equipe extends Thread {
    private int id;

    private final int MAX_GDD_PENDENTES = 2;
    private Semaphore filaGdds = new Semaphore(0);
    private Semaphore filaGddsLimite = new Semaphore(MAX_GDD_PENDENTES);

    private Competicao competicao;

    public Equipe(Competicao competicao, int id) {
        this.competicao = competicao;
        this.id = id;
    }

    public int getIdEquipe() { return this.id; }

    private class Designer extends Thread {
        private Random rng = new Random();
        private final int TEMPO_MINIMO = 500, TEMPO_MAXIMO = 1000;
        @Override
        public void run() {
            try {
                loop();
            } catch (InterruptedException e) {

            }
        }
        private void loop() throws InterruptedException {
            reportarEquipePronta();
            competicao.esperarInicio();
            for (int c = 0; !competicao.isFinalizada() && c < competicao.JOGOS_POR_EQUIPE; c++) {
                System.out.printf("Designer da equipe %d preparando GDD #%d\n", id, c + 1);
                Thread.sleep(TEMPO_MINIMO + rng.nextInt(TEMPO_MAXIMO));
                entregarGdd();
            }
        }
    }

    private class Desenvolvedor extends Thread {
        private Random rng = new Random();
        private final int TEMPO_MINIMO = 500, TEMPO_MAXIMO = 2000;
        @Override
        public void run() {
            try {
                loop();
            } catch (InterruptedException e) {

            }
        }
        private void loop() throws InterruptedException {
            for (int c = 0; !competicao.isFinalizada() && c < competicao.JOGOS_POR_EQUIPE; c++) {
                receberGdd();
                System.out.printf("Desenvolvedor da equipe %d preparando o jogo #%d\n", id, c + 1);
                Thread.sleep(TEMPO_MINIMO + rng.nextInt(TEMPO_MAXIMO));
                entregarJogo();
            }
        }
    }

    @Override
    public void run() {
        Designer designer = new Designer();
        Desenvolvedor desenvolvedor = new Desenvolvedor();
        designer.start();
        desenvolvedor.start();
        try {
            competicao.esperarTermino();
            designer.interrupt();
            desenvolvedor.interrupt();
            designer.join();
            desenvolvedor.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void entregarGdd() throws InterruptedException {
        filaGddsLimite.acquire();
        filaGdds.release();
    }

    private void receberGdd() throws InterruptedException {
        filaGdds.acquire();
    }

    private void entregarJogo() {
        filaGddsLimite.release();
        competicao.reportarJogoFinalizado(this);
    }

    private void reportarEquipePronta() {
        competicao.reportarEquipePronta(this);
    }
}
