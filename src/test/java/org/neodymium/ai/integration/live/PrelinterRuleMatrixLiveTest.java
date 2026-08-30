/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.integration.live;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestMethodOrder;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiInlinePlaybook;
import org.neodymium.ai.junit.AiLinter;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.playbook.linter.LinterCategory;
import org.neodymium.ai.playbook.linter.PlaybookLinterFinding;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Live integration test matrix verifying each of the 9 pre-flight linter quality rules in isolation
 * across diverse natural languages (German, Japanese, French, Spanish, English) and enterprise domains
 * (Healthcare, Cloud Infrastructure, Banking, HR Onboarding, Aviation, IoT, IAM, CI/CD, CRM).
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("LiveAPI")
@NeodymiumAiTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PrelinterRuleMatrixLiveTest extends BaseAiTest
{
    /**
     * Constructs a default PrelinterRuleMatrixLiveTest instance.
     */
    public PrelinterRuleMatrixLiveTest()
    {
    }

    /**
     * Rule 1: STEP_SPLITTING_CANDIDATE (German, Healthcare/Hospital Patient Management)
     *
     * @param session the active AI session
     */
    @Order(1)
    @AiLinter(true)
    @AiMode(ExecutionMode.LINTER_ONLY)
    @AiInlinePlaybook("""
        description: "Patientenaufnahme und Notfallkontakt in der Klinikverwaltung"
        steps: |
          Öffne die Patientenakte und trage "${notfallkontakt}" als Notfallkontakt ein.
        data:
          - testId: "med-1"
            notfallkontakt: "Dr. Maria Schmidt"
        """)
    @DisplayName("Rule 1: STEP_SPLITTING_CANDIDATE (German - Healthcare)")
    public void testStepSplittingCandidate_German_Medical(final AiSession session)
    {
        final Set<LinterCategory> categories = extractDetectedCategories(session);
        assertTrue(categories.contains(LinterCategory.STEP_SPLITTING_CANDIDATE), "Expected STEP_SPLITTING_CANDIDATE finding");
    }

    /**
     * Rule 2: MISSING_VISUAL_TAG (Japanese, Cloud DevOps & Cluster Monitor)
     *
     * @param session the active AI session
     */
    @Order(2)
    @AiLinter(true)
    @AiMode(ExecutionMode.LINTER_ONLY)
    @AiInlinePlaybook("""
        description: "Kubernetes クラスタヘルスチェック監視"
        steps: |
          クラスタステータスバッジは明るい緑色で、右上に配置されている。
        """)
    @DisplayName("Rule 2: MISSING_VISUAL_TAG (Japanese - Cloud DevOps)")
    public void testMissingVisualTag_Japanese_CloudDevOps(final AiSession session)
    {
        final Set<LinterCategory> categories = extractDetectedCategories(session);
        assertTrue(categories.contains(LinterCategory.MISSING_VISUAL_TAG), "Expected MISSING_VISUAL_TAG finding");
    }

    /**
     * Rule 3: AMBIGUOUS_AFFORDANCE (English, Banking & Wire Transfer)
     *
     * @param session the active AI session
     */
    @Order(3)
    @AiLinter(true)
    @AiMode(ExecutionMode.LINTER_ONLY)
    @AiInlinePlaybook("""
        description: "Corporate Treasury International Wire Transfer Approval"
        steps: |
          There is a security token card that allows approving international wire transfers.
        """)
    @DisplayName("Rule 3: AMBIGUOUS_AFFORDANCE (English - Banking Fintech)")
    public void testAmbiguousAffordance_English_BankingFintech(final AiSession session)
    {
        final Set<LinterCategory> categories = extractDetectedCategories(session);
        assertTrue(categories.contains(LinterCategory.AMBIGUOUS_AFFORDANCE), "Expected AMBIGUOUS_AFFORDANCE finding");
    }

    /**
     * Rule 4: VAGUE_TARGET (French, HR & Employee Onboarding Portal)
     *
     * @param session the active AI session
     */
    @Order(4)
    @AiLinter(true)
    @AiMode(ExecutionMode.LINTER_ONLY)
    @AiInlinePlaybook("""
        description: "Portail RH Validation des demandes"
        steps: |
          Cliquez sur le bouton.
        """)
    @DisplayName("Rule 4: VAGUE_TARGET (French - HR Onboarding)")
    public void testVagueTarget_French_HrOnboarding(final AiSession session)
    {
        final Set<LinterCategory> categories = extractDetectedCategories(session);
        assertTrue(categories.contains(LinterCategory.VAGUE_TARGET), "Expected VAGUE_TARGET finding");
    }

    /**
     * Rule 5: VAGUE_VERIFICATION (Spanish, Aviation & Flight Operations)
     *
     * @param session the active AI session
     */
    @Order(5)
    @AiLinter(true)
    @AiMode(ExecutionMode.LINTER_ONLY)
    @AiInlinePlaybook("""
        description: "Sistema de despacho y plan de vuelo operacional"
        steps: |
          Asegúrese de que el plan de vuelo se vea correcto y ordenado.
        """)
    @DisplayName("Rule 5: VAGUE_VERIFICATION (Spanish - Aviation)")
    public void testVagueVerification_Spanish_Aviation(final AiSession session)
    {
        final Set<LinterCategory> categories = extractDetectedCategories(session);
        assertTrue(categories.contains(LinterCategory.VAGUE_VERIFICATION), "Expected VAGUE_VERIFICATION finding");
    }

    /**
     * Rule 6: DANGLING_ANAPHORA (German, Smart Home & IoT Automation)
     *
     * @param session the active AI session
     */
    @Order(6)
    @AiLinter(true)
    @AiMode(ExecutionMode.LINTER_ONLY)
    @AiInlinePlaybook("""
        description: "Smart Home Gebäudeleittechnik Heizungssteuerung"
        steps: |
          Wähle das Wandthermostat und den Raumtemperatursensor aus.
          Stelle dessen Zieltemperatur auf 22 Grad ein.
        """)
    @DisplayName("Rule 6: DANGLING_ANAPHORA (German - Smart Home IoT)")
    public void testDanglingAnaphora_German_SmartHomeIot(final AiSession session)
    {
        final Set<LinterCategory> categories = extractDetectedCategories(session);
        assertTrue(categories.contains(LinterCategory.DANGLING_ANAPHORA), "Expected DANGLING_ANAPHORA finding");
    }

    /**
     * Rule 7: TEMPORAL_FLOW_ANOMALY (English, Cloud IAM & Security)
     *
     * @param session the active AI session
     */
    @Order(7)
    @AiLinter(true)
    @AiMode(ExecutionMode.LINTER_ONLY)
    @AiInlinePlaybook("""
        description: "Cloud IAM Role Security Revocation"
        steps: |
          Click "Confirm Revocation" inside the modal dialog.
          Click the "Configure Role" menu item to open settings.
        """)
    @DisplayName("Rule 7: TEMPORAL_FLOW_ANOMALY (English - Cloud IAM)")
    public void testTemporalFlowAnomaly_English_CloudIam(final AiSession session)
    {
        final Set<LinterCategory> categories = extractDetectedCategories(session);
        assertTrue(categories.contains(LinterCategory.TEMPORAL_FLOW_ANOMALY), "Expected TEMPORAL_FLOW_ANOMALY finding");
    }

    /**
     * Rule 8: HARDCODED_VOLATILE_DATA (English, CI/CD Pipeline Automation)
     *
     * @param session the active AI session
     */
    @Order(8)
    @AiLinter(true)
    @AiMode(ExecutionMode.LINTER_ONLY)
    @AiInlinePlaybook("""
        description: "Continuous Delivery Deployment Pipeline"
        steps: |
          Verify the pipeline run ID matches #BUILD-20260830-154829.
        """)
    @DisplayName("Rule 8: HARDCODED_VOLATILE_DATA (English - CI/CD Pipeline)")
    public void testHardcodedVolatileData_English_CicdPipeline(final AiSession session)
    {
        final Set<LinterCategory> categories = extractDetectedCategories(session);
        assertTrue(categories.contains(LinterCategory.HARDCODED_VOLATILE_DATA), "Expected HARDCODED_VOLATILE_DATA finding");
    }

    /**
     * Rule 9: INCOMPLETE_BRANCH_CLAUSE (German, Enterprise CRM Lead Management)
     *
     * @param session the active AI session
     */
    @Order(9)
    @AiLinter(true)
    @AiMode(ExecutionMode.LINTER_ONLY)
    @AiInlinePlaybook("""
        description: "Enterprise CRM Lead-Qualifizierung und Vertriebs-Workflow"
        steps: |
          Falls der Lead den Status "Qualified" besitzt.
        """)
    @DisplayName("Rule 9: INCOMPLETE_BRANCH_CLAUSE (German - Enterprise CRM)")
    public void testIncompleteBranchClause_German_EnterpriseCrm(final AiSession session)
    {
        final Set<LinterCategory> categories = extractDetectedCategories(session);
        assertTrue(categories.contains(LinterCategory.INCOMPLETE_BRANCH_CLAUSE), "Expected INCOMPLETE_BRANCH_CLAUSE finding");
    }

    /**
     * Helper to safely extract detected finding categories from session execution context.
     *
     * @param session the active AI session
     * @return set of detected linter categories
     */
    @SuppressWarnings("unchecked")
    private Set<LinterCategory> extractDetectedCategories(final AiSession session)
    {
        final ExecutionContext context = session.getExecutionContext();
        assertNotNull(context, "Expected active ExecutionContext");

        final List<PlaybookLinterFinding> findings = (List<PlaybookLinterFinding>) context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK_LINTER_FINDINGS);
        assertNotNull(findings, "Expected linter findings in execution context");
        assertFalse(findings.isEmpty(), "Expected at least one linter finding");

        return findings.stream()
            .map(PlaybookLinterFinding::category)
            .collect(Collectors.toSet());
    }
}
