/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.core.statements;

import java.util.Arrays;
import java.util.Collection;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import buildcraft.api.statements.IActionExternal;
import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IActionInternalSided;
import buildcraft.api.statements.IActionProvider;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.containers.IFillerStatementContainer;
import buildcraft.api.statements.containers.IRedstoneStatementContainer;
import buildcraft.api.tiles.IControllable;
import buildcraft.api.tiles.TilesAPI;

import buildcraft.core.BCCoreStatements;

public enum CoreActionProvider implements IActionProvider {
    INSTANCE;

    @Override
    public void addInternalActions(Collection<IActionInternal> res, IStatementContainer container) {
        if (container instanceof IRedstoneStatementContainer) {
            res.add(BCCoreStatements.ACTION_REDSTONE);
        }
    }

    @Override
    public void addInternalSidedActions(Collection<IActionInternalSided> actions, IStatementContainer container, EnumFacing side) { }

    @Override
    public void addExternalActions(Collection<IActionExternal> res, EnumFacing side, TileEntity tile) {
        IControllable controllable = tile.getCapability(TilesAPI.CAP_CONTROLLABLE, side.getOpposite());
        if (controllable != null) {
            Arrays.stream(BCCoreStatements.ACTION_MACHINE_CONTROL)
                    .filter(action -> controllable.setControlMode(action.mode, true))
                    .forEach(res::add);
        }
        if (tile instanceof IFillerStatementContainer) {
            res.add(BCCoreStatements.PATTERN_NONE);
            res.add(BCCoreStatements.PATTERN_CLEAR);
            res.add(BCCoreStatements.PATTERN_FILL);
            res.add(BCCoreStatements.PATTERN_BOX);
        }
    }
}
