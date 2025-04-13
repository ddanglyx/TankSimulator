// javac -classpath ".;C:\Program Files\lwjgl-release-3.3.6-custom\*" Bullet.java
// java -classpath ".;C:\Program Files\lwjgl-release-3.3.6-custom\*" Bullet

import org.lwjgl.opengl.GL11;

public class Bullet {
    private static final float SPEED = 0.1F;
    private static volatile Integer bulletTextureId = null;
    private float x;
    private float y;
    private float z;
    private float r;
    private float g;
    private float b;
    private float directionX;
    private float directionY;
    private float directionZ;

    // Add this method to load texture in OpenGL context
    private static synchronized int getOrLoadTexture() {
        if (bulletTextureId == null) {
            bulletTextureId = ImageLoader.loadImage("bullet.png");
        }
        return bulletTextureId;
    }

    public Bullet(Tank tank, Terrain terrain) {
        // set bullet color
        this.r = tank.getR();
        this.g = tank.getG();
        this.b = tank.getB();

        // get tank position
        this.x = tank.getX();
        this.y = tank.getY() + 2.0f;

        // calculate angles of turret
        float turretAngle = (float)Math.toRadians(tank.getCombinedTurretAngle());

        // initialize direction vector
        float[] direction = {0.0F, 0.0F, 1.0F};

        // Rotate based on turret angle
        direction = rotateY(direction, turretAngle);
        direction = normalize(direction);

        this.directionX = direction[0];
        this.directionY = 0.0f;
        this.directionZ = direction[2];

        // Increase spawn offset to move bullet further forward
        float tankLength = tank.getTankLength();
        float spawnOffset = tankLength * 0.75f; // Increased offset multiplier

        // Move bullet spawn position forward
        this.x = tank.getX() + (spawnOffset * this.directionX);
        this.z = tank.getZ() + (spawnOffset * this.directionZ);

        System.out.println("Created bullet for " + (tank.getR() > tank.getB() ? "Player 2" : "Player 1"));
        System.out.println("Position: " + this.x + "," + this.y + "," + this.z);
        System.out.println("Direction: " + this.directionX + "," + this.directionY + "," + this.directionZ);
    }

    // bullet constructor for remote bullets
    public Bullet(float x, float y, float z, float directionX, float directionY, float directionZ, float r, float g, float b) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.directionX = directionX;
        this.directionY = directionY;
        this.directionZ = directionZ;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public void update() {
        this.x += this.directionX * SPEED;
        this.y += this.directionY * SPEED;
        this.z += this.directionZ * SPEED;
    }

    public void render() {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, z);
        GL11.glColor3f(r, g, b);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, getOrLoadTexture());

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex3f(-0.1f, -0.1f, 0.0f);
        GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex3f( 0.1f, -0.1f, 0.0f);
        GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex3f( 0.1f,  0.1f, 0.0f);
        GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex3f(-0.1f,  0.1f, 0.0f);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    // Getters
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getDirectionX() { return directionX; }
    public float getDirectionY() { return directionY; }
    public float getDirectionZ() { return directionZ; }
    public float getR() { return r; }
    public float getG() { return g; }
    public float getB() { return b; }

    // Helper methods
    private float[] rotateX(float[] v, float angle) {
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);
        return new float[] {
            v[0],
            v[1] * cos - v[2] * sin,
            v[1] * sin + v[2] * cos
        };
    }

    private float[] rotateY(float[] v, float angle) {
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);
        return new float[] {
            v[0] * cos + v[2] * sin,
            v[1],
            -v[0] * sin + v[2] * cos
        };
    }

    private float[] normalize(float[] v) {
        float length = (float)Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new float[] {
            v[0] / length,
            v[1] / length,
            v[2] / length
        };
    }

    private float getAverageHeight(Tank tank, Terrain terrain) {
        int numWheels = tank.getNumWheelsPerSide();
        float tankLength = tank.getTankLength();
        float wheelSpacing = tankLength / (numWheels - 1);

        float[] leftHeights = new float[numWheels];
        float[] rightHeights = new float[numWheels];

        for (int i = 0; i < numWheels; i++) {
            float offset = -tankLength/2 + i * wheelSpacing;
            leftHeights[i] = terrain.getTerrianHeightAt(x - 0.9f, z + offset);
            rightHeights[i] = terrain.getTerrianHeightAt(x + 0.9f, z + offset);
        }

        float sum = 0;
        for (int i = 0; i < numWheels; i++) {
            sum += leftHeights[i] + rightHeights[i];
        }

        return sum / (numWheels * 2);
    }
}