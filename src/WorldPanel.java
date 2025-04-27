import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WorldPanel extends JPanel {
    private List<Creature> creatures = new ArrayList<>();
    private List<Creature> eliteCreatures = new ArrayList<>();

    private final int simWidth;
    private final int simHeight;
    private final int scale;

    public WorldPanel(List<Creature> creatures, int simWidth, int simHeight, int scale) {
        setCreatures(creatures);
        this.simWidth = simWidth;
        this.simHeight = simHeight;
        this.scale = scale;
        setPreferredSize(new Dimension(simWidth * scale, simHeight * scale));
    }
    
    public void setEliteCreatures(List<Creature> elites) {
        this.eliteCreatures = new ArrayList<>(elites);
    }

    public void setCreatures(List<Creature> updatedCreatures) {
        this.creatures = new ArrayList<>(updatedCreatures);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (Creature c : creatures) {
            int px = (c.x + simWidth / 2) * scale;
            int py = (c.y + simHeight / 2) * scale;

            g.setColor(c.isFood ? Color.BLACK : colorFromGenes(c));
            g.fillOval(px, py, scale, scale);
            
            // Draw a border if this is an elite creature
            if (c.health > 100) {
                //g.setColor(Color.GREEN); // Border color
                //g.drawOval(px, py, scale, scale);
                //for(double gene : c.getGeneticCode())
                //	System.out.print(gene + ",");
                //System.out.println();
            }
            
            if (c.health < 10 && !c.isFood) {
                //g.setColor(Color.RED); // Border color
                //g.drawOval(px, py, scale, scale);
            }
            
            if (!c.isFood) {
            	Creature nearestFood = c.getNearestFood();
            	
                if (nearestFood != null) {
                    int fx = (nearestFood.x + simWidth / 2) * scale;
                    int fy = (nearestFood.y + simHeight / 2) * scale;

                   // g.setColor(Color.BLUE); // Direction indicator color
                   // drawArrow(g, px + scale/2, py + scale/2, fx + scale/2, fy + scale/2);
                }
            }
        }
    }
    
    private void drawArrow(Graphics g, int x1, int y1, int x2, int y2) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(1)); // Thicker line if you want

        g2.drawLine(x1, y1, x2, y2);

        // Optional: add arrowhead (simple)
        int dx = x2 - x1, dy = y2 - y1;
        double D = Math.sqrt(dx*dx + dy*dy);
        if (D == 0) return; // avoid division by zero
        double xm = x2 - (dx / D) * 5;
        double ym = y2 - (dy / D) * 5;
        double xn = xm + (dy / D) * 3;
        double yn = ym - (dx / D) * 3;
        double xp = xm - (dy / D) * 3;
        double yp = ym + (dx / D) * 3;

        int[] xpoints = {x2, (int) xn, (int) xp};
        int[] ypoints = {y2, (int) yn, (int) yp};

        g.fillPolygon(xpoints, ypoints, 3);
    }

    private Color colorFromGenes(Creature c) {
        double[] genes = c.getGeneticCode();
        //for(double g : genes)
        //	System.out.print(g + ",");
        //System.out.println();
        //if (genes.length < 9) return Color.GRAY;
        
    

        double r = averageSigmoid(genes, 0, 2);
        double g = averageSigmoid(genes, 2, 4);
        double b = averageSigmoid(genes, 4, 6);

        r = boost(r);
        g = boost(g);
        b = boost(b);

        return new Color((int)(r * 255), (int)(g * 255), (int)(b * 255));
    }

    private double averageSigmoid(double[] genes, int start, int end) {
        double sum = 0;
        for (int i = start; i < end && i < genes.length; i++) {
            sum += sigmoid(genes[i]);
        }
        return sum / (end - start);
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    private double boost(double val) {
        return Math.pow(val, 0.6);
    }
}
