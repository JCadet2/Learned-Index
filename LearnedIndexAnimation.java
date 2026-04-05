import javax.swing.*;
import java.awt.*;

public class LearnedIndexAnimation extends JPanel{
    int step = 0;
    int[] arr = {10, 15, 21, 30, 44, 52, 60, 71};
    int predictedIndex = 5;

    public LearnedIndexAnimation(){
        Timer timer =
        new Timer(1100,e->{

            step++;

            if (step > 6){
                step = 0;
            }

            repaint();
        });

        timer.start();
    }

    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;

        g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);

        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("How a Learned Index Answers a Query", 180, 40);

        drawArray(g);

        if (step >= 1){
            drawQuery(g);
        }

        if (step >= 2){
            drawPrediction(g);
        }

        if (step >= 3){
            drawArrow(g);
        }

        if (step >= 4){
            drawSearchWindow(g);
        }

        if (step >= 5){
            drawAnswer(g);
        }

    }

    void drawArray(Graphics g){
        int x = 70;

        for (int i = 0; i < arr.length; i++){
            if (step >= 4 && i >= 4 && i <= 6){
                g.setColor(new Color(180, 220, 255));
            }else{
                g.setColor(Color.WHITE);
            }

            g.fillRect(x, 90, 60, 45);
            g.setColor(Color.BLACK);
            g.drawRect(x, 90, 60,45);
            g.drawString("" + arr[i], x+18, 118);

            x += 70;
        }
    }

    void drawQuery(Graphics g){
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("Query: 50", 70, 190);
    }

    void drawPrediction(Graphics g){
        g.drawString("Model predicts position near 52", 70, 230);
    }

    void drawArrow(Graphics g){
        int x = 70 + predictedIndex*70 + 30;

        g.setColor(Color.RED);
        g.drawLine(x, 70, x, 90);

        g.fillPolygon(new int[]{x - 6, x + 6, x}, new int[]{80, 80, 90}, 3);

        g.setColor(Color.BLACK);
        g.drawString("Predicted", x - 35, 60);
    }

    void drawSearchWindow(Graphics g){
        g.drawString("Local search window", 70, 270);
    }

    void drawAnswer(Graphics g){
        g.setColor(new Color(170, 240, 170));
        g.fillRect(70, 300, 160, 45);

        g.setColor(Color.BLACK);
        g.drawRect(70, 300, 160, 45);

        g.setFont(new Font("Arial", Font.BOLD, 18));

        g.drawString("Answer: 44",  95, 330);
    }

    public static void main(String[] args){
        JFrame frame = new JFrame("Learned Index Animation");
        LearnedIndexAnimation panel = new LearnedIndexAnimation();

        panel.setPreferredSize(new Dimension(650, 400));

        frame.add(panel);
        frame.pack();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

}