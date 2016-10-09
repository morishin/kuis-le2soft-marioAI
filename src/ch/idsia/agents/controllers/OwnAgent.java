/*
 * Copyright (c) 2009-2010, Sergey Karakovskiy and Julian Togelius
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Mario AI nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package ch.idsia.agents.controllers;

import ch.idsia.agents.Agent;
import ch.idsia.benchmark.mario.engine.GeneralizerLevelScene;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;
import ch.idsia.benchmark.mario.environments.Environment;

/**
 * Created by IntelliJ IDEA.
 * User: Sergey Karakovskiy, sergey.karakovskiy@gmail.com
 * Date: Apr 8, 2009
 * Time: 4:03:46 AM
 */

public class OwnAgent extends BasicMarioAIAgent implements Agent {
    private class TimeAndDistance {
        public int time;
        public int distance;
        public TimeAndDistance(int time, int distance) {
            this.time = time;
            this.distance = distance;
        }

        public String toString() {
            return "time: " + time + " " + distancePassedCells;
        }
    }

    int stopCounter = 0;
    private TimeAndDistance previousCoordinate = new TimeAndDistance(0, 0);
    private boolean shouldLowJump = false;

    public OwnAgent() {
        super("OwnAgent");
        reset();
    }

    public void reset() {
        action = new boolean[Environment.numberOfKeys];
        action[Mario.KEY_RIGHT] = true;
        stopCounter = 0;
        shouldLowJump = false;
        previousCoordinate.time = timeSpent;
        previousCoordinate.distance = distancePassedCells;
    }

    private boolean isObstacle(int r, int c) {
        return getReceptiveFieldCellValue(r, c) == GeneralizerLevelScene.BRICK
                || getReceptiveFieldCellValue(r, c) == GeneralizerLevelScene.BORDER_CANNOT_PASS_THROUGH
                || getReceptiveFieldCellValue(r, c) == GeneralizerLevelScene.FLOWER_POT_OR_CANNON
                || getReceptiveFieldCellValue(r, c) == GeneralizerLevelScene.LADDER;
    }

    private boolean isEmpty(int r, int c) {
        return getReceptiveFieldCellValue(r, c) == 0;
    }

    private boolean shouldJump() {
        if (isMarioOnGround && !isMarioAbleToJump) {
            //着地した瞬間は再ジャンプのために一度離す必要がある
            return false;
        }
        if (!isMarioOnGround) {
            // 常にハイジャンプ
            return true;
        }
        return isObstacle(marioEgoRow, marioEgoCol + 1)
                || getEnemiesCellValue(marioEgoRow, marioEgoCol + 2) != Sprite.KIND_NONE
                || getEnemiesCellValue(marioEgoRow, marioEgoCol + 1) != Sprite.KIND_NONE
                || isEmpty(marioEgoRow + 1, marioEgoCol + 1);
    }

    private boolean shouldCancelJumpAndBack() {
        if (isMarioAbleToJump || !isMarioOnGround) { return false; }
        return isOverGround(marioEgoRow, marioEgoCol)
                && !isOverGround(marioEgoRow, marioEgoCol + 1);
    }

    private boolean shouldLowJump() {
        return shouldLowJump;
    }

    private boolean isOverGround(int r, int c) {
        return isObstacle(r + 1, c)
                || isObstacle(r + 2, c)
                || isObstacle(r + 3, c)
                || isObstacle(r + 4, c)
                || isObstacle(r + 5, c)
                || isObstacle(r + 6, c)
                || isObstacle(r + 7, c)
                || isObstacle(r + 8, c)
                || isObstacle(r + 9, c);
    }

    private boolean isTowardHole() {
        return !isOverGround(marioEgoRow, marioEgoCol + 1)
                || !isOverGround(marioEgoRow, marioEgoCol + 2)
                || !isOverGround(marioEgoRow, marioEgoCol + 3)
                || !isOverGround(marioEgoRow, marioEgoCol + 4);
    }

    public boolean[] getAction() {
        beginFrame();

        action[Mario.KEY_SPEED] = isTowardHole();

        if (shouldCancelJumpAndBack()) {
            action[Mario.KEY_LEFT] = true;
            action[Mario.KEY_RIGHT] = false;
            action[Mario.KEY_JUMP] = false;
            log("cancel jump back");
        } else if (shouldLowJump) {
            action[Mario.KEY_LEFT] = false;
            action[Mario.KEY_RIGHT] = true;
            action[Mario.KEY_JUMP] = false;
            if (shouldLowJump()) {
                action[Mario.KEY_LEFT] = true;
                action[Mario.KEY_RIGHT] = false;
            }
            shouldLowJump = false;
            log("cancel jump");
        } else if (shouldJump()) {
            action[Mario.KEY_LEFT] = false;
            action[Mario.KEY_RIGHT] = true;
            action[Mario.KEY_JUMP] = true;
            shouldLowJump = stopCounter > 1;
            log("jump");
        } else {
            action[Mario.KEY_LEFT] = false;
            action[Mario.KEY_RIGHT] = true;
            action[Mario.KEY_JUMP] = false;
            log("default");
        }

        endFrame();
        return action;
    }

    private void beginFrame() {
        if (timeSpent == previousCoordinate.time) { return; }

        if (distancePassedCells == previousCoordinate.distance) {
            stopCounter += 1;
        } else {
            stopCounter = 0;
        }
    }

    private void endFrame() {
        if (timeSpent == previousCoordinate.time) { return; }

        previousCoordinate.time = timeSpent;
        previousCoordinate.distance = distancePassedCells;
    }

    private void log(String message) {
        System.out.println(message);
    }
}
