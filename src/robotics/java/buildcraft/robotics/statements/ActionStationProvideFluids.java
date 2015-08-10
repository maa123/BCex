/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.robotics.statements;

import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementParameterItemStack;
import buildcraft.core.lib.utils.StringUtils;
import buildcraft.core.statements.BCStatement;

public class ActionStationProvideFluids extends BCStatement implements IActionInternal {

    public ActionStationProvideFluids() {
        super("buildcraft:station.provide_fluids");
        setLocation("buildcraftrobotics:triggers/action_station_provide_fluids");
    }

    @Override
    public String getDescription() {
        return StringUtils.localize("gate.action.station.povide_fluids");
    }

    // @Override
    // public void registerIcons(TextureAtlasSpriteRegister iconRegister) {
    // icon = iconRegister.registerIcon("buildcraftrobotics:triggers/action_station_provide_fluids");
    // }

    @Override
    public int maxParameters() {
        return 3;
    }

    @Override
    public IStatementParameter createParameter(int index) {
        return new StatementParameterItemStack();
    }

    @Override
    public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {

    }
}
