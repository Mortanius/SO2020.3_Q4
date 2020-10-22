package main;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Competicao implements  Runnable {
    public final int TOTAL_EQUIPES = 5;
    public final int JOGOS_POR_EQUIPE = 10;

    private Equipe[] equipes;
    private Map<Equipe, Integer> jogosPorEquipe;

    private Semaphore equipesProntas = new Semaphore(0);
    private Semaphore comecar = new Semaphore(0);
    private Semaphore finalizar = new Semaphore(0);

    private Lock equipeFinalizouJogo = new ReentrantLock();

    private boolean finalizada;
    private Semaphore esperaVencedor = new Semaphore(0);

    public boolean isFinalizada() { return finalizada; }

    public Competicao() {
        equipes = new Equipe[TOTAL_EQUIPES];
        jogosPorEquipe = new HashMap<>();
        for (int c = 0; c < equipes.length; c++) {
            equipes[c] = new Equipe(this, c);
            jogosPorEquipe.put(equipes[c], 0);
        }
    }

    @Override
    public void run() {
        for (Equipe equipe : equipes)
            equipe.start();
        try {
            equipesProntas.acquire(TOTAL_EQUIPES);
            iniciarCompeticao();
            for (Equipe equipe : equipes)
                equipe.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<Equipe> lista_equipes = Arrays.asList(equipes);
        lista_equipes.sort((e1, e2) -> {
            Integer j1 = jogosPorEquipe.get(e1), j2 = jogosPorEquipe.get(e2);
            if (j1 > j2)
                return -1;
            else if (j1 == j2)
                return Integer.compare(e1.getIdEquipe(), e2.getIdEquipe());
            else
                return 1;
        });
        for (Equipe e : lista_equipes)
            System.out.printf("Equipe %d finalizou %d jogos\n", e.getIdEquipe(), jogosPorEquipe.get(e));
    }

    public void esperarInicio() throws InterruptedException {
        comecar.acquire();
    }

    public void esperarTermino() throws InterruptedException {
        finalizar.acquire();
    }

    private void iniciarCompeticao() {
        comecar.release(TOTAL_EQUIPES);
    }

    private void finalizarCompeticao() {
        finalizada = true;
        finalizar.release(TOTAL_EQUIPES);
    }

    public void reportarEquipePronta(Equipe e) {
        if (jogosPorEquipe.containsKey(e))
            equipesProntas.release();
    }

    public void reportarJogoFinalizado(Equipe e) {
        equipeFinalizouJogo.lock();
        Integer jogosFinalizados = jogosPorEquipe.get(e);
        if (jogosFinalizados == null) {
            equipeFinalizouJogo.unlock();
            return;
        }
        jogosFinalizados++;
        jogosPorEquipe.put(e, jogosFinalizados);
        System.out.printf("Competicao: Equipe %d finalizou %d jogos\n", e.getIdEquipe(), jogosFinalizados);
        if (jogosFinalizados == JOGOS_POR_EQUIPE) {
            finalizarCompeticao();
        }
        equipeFinalizouJogo.unlock();
    }
}
