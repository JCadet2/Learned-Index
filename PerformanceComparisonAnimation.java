import javax.swing.*;
import java.awt.*;

/*
OVERALL DESCRIPTION 
Animated a bar chart comparing query performance
(ns per query) of four data structures (Binary Search, Learned Index,
Skip List, and Y-fast Trie) on three different 1M element datasets
(Uniform, Sequential, Skewed).
*/

public class PerformanceComparisonAnimation extends JPanel{
    int dataset = 0; // Auto advances after animation completes
    int animationProgress = 0; // Controls animated bar growth
    int pauseFrames = 0; // Small delay after each animation
    int maxValue = 0;

    String[] names =
    {"Binary", "Learned", "SkipList", "y-fast"};

    // 1M ELEMENT RESULTS ONLY (sample output from LearnedIndexes.java)
    int[][] data =
    {
        {244, 650, 722, 507},   // Uniform
        {246, 589, 1021, 632},    // Sequential 
        {224, 1136, 1271, 416}     // Skewed 
    };

    String[] sizes =
    {
     "Uniform distribution (1M)",
     "Sequential distribution (1M)",
     "Skewed distribution (1M)"
    };

    Color[] colors =
    {
        new Color(60,120,240),
        new Color(60,200,120),
        new Color(240,90,90),
        new Color(240,170,60)
    };

    public PerformanceComparisonAnimation(){
        updateMax();

        Timer timer =
        new Timer(40,e->{

            // Growth speed tied to dataset magnitude so animation feels consistent
            animationProgress += Math.max(8,maxValue/80);

            // Delay before advancing to next dataset
            if (animationProgress >= maxValue){
                pauseFrames++;

                if (pauseFrames > 50){
                    dataset++;

                    if (dataset >= data.length){
                        dataset = data.length - 1;
                        return;
                    }

                    animationProgress = 0;
                    pauseFrames = 0;

                    updateMax();
                }
            }

            repaint();
        });

        timer.start();
    }

    // Recompute scaling when switching datasets
    private void updateMax(){
        maxValue = 0;

        for (int val : data[dataset]){
            if (val > maxValue)
                maxValue = val;
        }
    }

    protected void paintComponent(Graphics g){
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D)g;

        g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON
        );

        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Query Performance Comparison", 240, 40);

        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString(sizes[dataset], 270, 70);

        drawAxes(g);
        drawBars(g);
    }

    void drawAxes(Graphics g){
        g.drawLine(100, 350, 720, 350);
        g.drawLine(100, 350, 100, 80);

        // Fixed grid lines and labels for easier comparison between datasets
        for (int i = 0; i <= 5; i++){
            int y =  350 - i*50;

            g.setColor( new Color(220, 220, 220));
            g.drawLine(100, y, 720, y);
            g.setColor(Color.BLACK);

            // Static scale chosen for readablitiy rahter than perfect scaling
            g.drawString("" + (i * 250), 55, y + 5);
        }

        g.drawString("ns/query", 30, 70);
    }

    void drawBars(Graphics g){
        int barWidth = 90;

        for (int i = 0; i < 4; i++){
            int value = data[dataset][i];

            // Bar height tied to animation progress for growth effect
            int height = Math.min(value,animationProgress) * 220 / maxValue;

            // Fixed spacing keeps layout identical across datasets
            int x = 140 + (i * 140);
            int y = 350 - height;

            g.setColor(colors[i]);
            g.fillRect(x, y, barWidth, height);

            g.setColor(Color.BLACK);
            g.drawRect(x, y, barWidth, height);

            g.drawString(names[i], x + 10, 375);

            // Values only appear after bar finishes growing for cleaner animation
            if (animationProgress >= maxValue){
                g.drawString(value + " ns", x + 15, y - 10);
            }

        }

    }

    public static void main(String[] args){
        JFrame frame = new JFrame("Performance Animation");
        PerformanceComparisonAnimation panel = new PerformanceComparisonAnimation();

        panel.setPreferredSize(new Dimension(820,450));

        frame.add(panel);
        frame.pack();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

}