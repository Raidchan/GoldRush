package net.raiid.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public class Region {

    private Location pos1;
    private Location pos2;

    public Region(Location pos1, Location pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public Location getPos1() {
        return this.pos1;
    }
    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public Location getPos2() {
        return this.pos2;
    }
    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public boolean isWithin(Location loc) {
        if (loc == null || this.pos1 == null || this.pos2 == null) return false;
        if (!sameWorld(loc.getWorld(), this.pos1.getWorld()) || !sameWorld(loc.getWorld(), this.pos2.getWorld())) return false;

        double minX = Math.min(this.pos1.getX(), this.pos2.getX());
        double maxX = Math.max(this.pos1.getX(), this.pos2.getX());
        double minY = Math.min(this.pos1.getY(), this.pos2.getY());
        double maxY = Math.max(this.pos1.getY(), this.pos2.getY());
        double minZ = Math.min(this.pos1.getZ(), this.pos2.getZ());
        double maxZ = Math.max(this.pos1.getZ(), this.pos2.getZ());

        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }

    public boolean isWithin(Entity entity) {
        return this.isWithin(entity.getLocation());
    }

    private boolean sameWorld(World a, World b) {
        return a != null && b != null && a.equals(b);
    }

    /**
     * �����ړ��� from/to �̂ǂ���ɂ������Ă��Ȃ����A�ԂŔ������؂����ꍇ���E�����߂�
     * �����ifrom��to�j��AABB�̌�������islab�@�j�B
     * ���ꃏ�[���h�łȂ���� false�B
     */
    public boolean intersectsSegment(Location from, Location to) {
        if (from == null || to == null || pos1 == null || pos2 == null) return false;
        World w = pos1.getWorld();
        if (!sameWorld(from.getWorld(), w) || !sameWorld(to.getWorld(), w)) return false;

        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        double sx = from.getX(), sy = from.getY(), sz = from.getZ();
        double dx = to.getX() - sx, dy = to.getY() - sy, dz = to.getZ() - sz;

        double tmin = 0.0, tmax = 1.0;

        // X
        if (Math.abs(dx) < 1e-9) {
            if (sx < minX || sx > maxX) return false;
        } else {
            double ood = 1.0 / dx;
            double t1 = (minX - sx) * ood;
            double t2 = (maxX - sx) * ood;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            if (t1 > tmin) tmin = t1;
            if (t2 < tmax) tmax = t2;
            if (tmin > tmax) return false;
        }

        // Y
        if (Math.abs(dy) < 1e-9) {
            if (sy < minY || sy > maxY) return false;
        } else {
            double ood = 1.0 / dy;
            double t1 = (minY - sy) * ood;
            double t2 = (maxY - sy) * ood;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            if (t1 > tmin) tmin = t1;
            if (t2 < tmax) tmax = t2;
            if (tmin > tmax) return false;
        }

        // Z
        if (Math.abs(dz) < 1e-9) {
            if (sz < minZ || sz > maxZ) return false;
        } else {
            double ood = 1.0 / dz;
            double t1 = (minZ - sz) * ood;
            double t2 = (maxZ - sz) * ood;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            if (t1 > tmin) tmin = t1;
            if (t2 < tmax) tmax = t2;
            if (tmin > tmax) return false;
        }

        return tmax >= tmin && tmax >= 0.0 && tmin <= 1.0;
    }
}
