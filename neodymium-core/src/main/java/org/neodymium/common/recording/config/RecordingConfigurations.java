/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.common.recording.config;

import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Mutable;

/**
 * Configuration interface, which contains the variables needed for both gif and video recording.
 * 
 * @author Xceptance Software Technologies
 */
@LoadPolicy(LoadType.MERGE)
public abstract interface RecordingConfigurations extends Mutable
{
    public boolean enableFilming();

    public boolean filmAutomatically();

    public int oneImagePerMilliseconds();

    public String tempFolderToStoreRecording();

    public boolean deleteRecordingsAfterAddingToAllureReport();

    public boolean deleteTempRecordings();

    public boolean appendAllRecordingsToAllureReport();

    public boolean attachOnlyOnFailure();

    public double imageQuality();

    public double imageScaleFactor();

    public String format();

    public boolean logInformationAboutRecording();
}
