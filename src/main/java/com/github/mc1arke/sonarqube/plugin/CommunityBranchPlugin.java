/*
 * Copyright (C) 2019 Michael Clarke
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package com.github.mc1arke.sonarqube.plugin;

import com.github.mc1arke.sonarqube.plugin.ce.CommunityBranchEditionProvider;
import com.github.mc1arke.sonarqube.plugin.ce.CommunityReportAnalysisComponentProvider;
import com.github.mc1arke.sonarqube.plugin.scanner.CommunityBranchConfigurationLoader;
import com.github.mc1arke.sonarqube.plugin.scanner.CommunityBranchParamsValidator;
import com.github.mc1arke.sonarqube.plugin.scanner.CommunityProjectBranchesLoader;
import com.github.mc1arke.sonarqube.plugin.scanner.CommunityProjectPullRequestsLoader;
import com.github.mc1arke.sonarqube.plugin.server.CommunityBranchFeatureExtension;
import com.github.mc1arke.sonarqube.plugin.server.CommunityBranchSupportDelegate;
import org.sonar.api.CoreProperties;
import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.Version;
import org.sonar.core.extension.CoreExtension;

/**
 * @author Michael Clarke
 */
public class CommunityBranchPlugin implements Plugin, CoreExtension {

    @Override
    public String getName() {
        return "Community Branch Plugin";
    }

    @Override
    public void load(CoreExtension.Context context) {
        if (SonarQubeSide.COMPUTE_ENGINE == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(CommunityReportAnalysisComponentProvider.class, CommunityBranchEditionProvider.class);
        } else if (SonarQubeSide.SERVER == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(CommunityBranchFeatureExtension.class, CommunityBranchSupportDelegate.class);
        }

        context.addExtensions(
            /* org.sonar.db.purge.PurgeConfiguration uses the value for the this property if it's configured, so it only
            needs to be specified here, but doesn't need any additional classes to perform the relevant purge/cleanup
             */
                PropertyDefinition.builder(FutureProperties.DAYS_BEFORE_DELETING_INACTIVE_BRANCHES_AND_PRS)
                        .deprecatedKey("sonar.dbcleaner.daysBeforeDeletingInactiveBranches")
                        .name("Number of days before purging inactive short living branches").description(
                        "Short living branches are permanently deleted when there are no analysis for the configured number of days.")
                        .category(context.getRuntime().getApiVersion().isGreaterThanOrEqual(Version.create(8, 0)) ?
                                  FutureProperties.CATEGORY_HOUSEKEEPING : CoreProperties.CATEGORY_GENERAL).subCategory(
                        context.getRuntime().getApiVersion().isGreaterThanOrEqual(Version.create(8, 0)) ?
                        FutureProperties.SUBCATEGORY_GENERAL : LegacyProperties.SUBCATEGORY_DATABASE_CLEANER)
                        .defaultValue("30")
                        .type(PropertyType.INTEGER).build(),

                //the name and description shown on the UI are automatically loaded from core.properties so don't need to be specified here
                PropertyDefinition.builder(LegacyProperties.LONG_LIVED_BRANCHES_REGEX).onQualifiers(Qualifiers.PROJECT)
                        .category(CoreProperties.CATEGORY_GENERAL).subCategory(
                        context.getRuntime().getApiVersion().isGreaterThanOrEqual(Version.create(8, 0)) ?
                        FutureProperties.SUBCATEGORY_BRANCHES_AND_PULL_REQUESTS : LegacyProperties.SUBCATEGORY_BRANCHES)
                        .defaultValue(CommunityBranchConfigurationLoader.DEFAULT_BRANCH_REGEX).build());

    }

    @Override
    public void define(Plugin.Context context) {
        if (SonarQubeSide.SCANNER == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(CommunityProjectBranchesLoader.class, CommunityProjectPullRequestsLoader.class,
                                  CommunityBranchConfigurationLoader.class, CommunityBranchParamsValidator.class);
        }
    }

    public static class LegacyProperties implements SonarqubeCompatibility.Major8.Minor0 {

        public static final String LONG_LIVED_BRANCHES_REGEX = "sonar.branch.longLivedBranches.regex";

        private static final String SUBCATEGORY_BRANCHES = "Branches";

        private static final String SUBCATEGORY_DATABASE_CLEANER = "databaseCleaner";

        private LegacyProperties() {
            super();
        }

    }

    private static class FutureProperties implements SonarqubeCompatibility.Major8.Minor1 {
        private static final String SUBCATEGORY_BRANCHES_AND_PULL_REQUESTS = "branchesAndPullRequests";

        private static final String SUBCATEGORY_GENERAL = "general";

        private static final String CATEGORY_HOUSEKEEPING = "housekeeping";

        private static final String DAYS_BEFORE_DELETING_INACTIVE_BRANCHES_AND_PRS =
                "sonar.dbcleaner.daysBeforeDeletingInactiveBranchesAndPRs";

        private FutureProperties() {
            super();
        }
    }
}
