/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.aura.chat.service;

import com.xceptance.aura.chat.dto.ChatMessageDto;
import com.xceptance.aura.chat.dto.ChatSessionDto;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link AuraChatSessionService}.
 *
 * @author AI-generated: Gemini 4 Hydrogen
 * @author Xceptance GmbH 2026
 */
public class AuraChatSessionServiceTest
{
    @TempDir
    Path tempDir;

    @Test
    public void testCreatesDefaultSessionAndPersistsMessages()
    {
        final AuraChatSessionService service = new AuraChatSessionService(tempDir.toFile());

        final List<ChatSessionDto> sessions = service.getSessions();
        Assertions.assertEquals(1, sessions.size());

        final ChatSessionDto created = service.createSession("Debug Run");
        Assertions.assertNotNull(created.id);
        Assertions.assertEquals("Debug Run", created.name);

        service.addMessage(created.id, new ChatMessageDto("user", "List all tests"));
        service.addMessage(created.id, new ChatMessageDto("ai", "Found 2 tests.", "[Tool Call] list_tests"));
        service.renameSession(created.id, "Test Discovery");

        final Path jsonFile = tempDir.resolve(created.id + ".json");
        Assertions.assertTrue(Files.exists(jsonFile));

        // Reload from disk in a fresh service instance to verify JSON compatibility
        final AuraChatSessionService reloadedService = new AuraChatSessionService(tempDir.toFile());
        final ChatSessionDto reloaded = reloadedService.getSession(created.id);
        Assertions.assertNotNull(reloaded);
        Assertions.assertEquals("Test Discovery", reloaded.name);
        Assertions.assertEquals(2, reloaded.messages.size());
        Assertions.assertEquals("user", reloaded.messages.get(0).role);
        Assertions.assertEquals("List all tests", reloaded.messages.get(0).content);
        Assertions.assertEquals("ai", reloaded.messages.get(1).role);
        Assertions.assertEquals("[Tool Call] list_tests", reloaded.messages.get(1).thinking);

        reloadedService.deleteSession(created.id);
        Assertions.assertFalse(Files.exists(jsonFile));
        Assertions.assertTrue(reloadedService.getSessions().stream().noneMatch(s -> created.id.equals(s.id)));
    }

    @Test
    public void testRejectsPathTraversalAndUnknownIdsWithoutMutatingDefaultSession()
    {
        final AuraChatSessionService service = new AuraChatSessionService(tempDir.toString());
        final ChatSessionDto defaultSession = service.getSessions().get(0);
        final String originalName = defaultSession.name;

        Assertions.assertThrows(IllegalArgumentException.class, () -> service.findSession("../evil"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.findSession("sub/dir"));
        Assertions.assertTrue(service.findSession("non-existent-id").isEmpty());
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.renameSession("../evil", "Hijacked"));
        Assertions.assertNull(service.renameSession("non-existent-id", "Hijacked"));

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.addMessage("../evil", new ChatMessageDto("user", "Should not land in default")));
        service.addMessage("unknown-id", new ChatMessageDto("user", "Should not land in default"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.deleteSession("../outside"));

        final ChatSessionDto refreshedDefault = service.getSession(defaultSession.id);
        Assertions.assertEquals(originalName, refreshedDefault.name);
        Assertions.assertTrue(refreshedDefault.messages.isEmpty());
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> refreshedDefault.messages.add(new ChatMessageDto("user", "direct mutation")));
    }

    @Test
    public void testGetSessionsSortedByRecencyUpdatedAt() throws Exception
    {
        final AuraChatSessionService service = new AuraChatSessionService(tempDir.toFile());
        final ChatSessionDto zed = service.createSession("Zed Oldest First");
        Thread.sleep(5L);
        final ChatSessionDto alpha = service.createSession("Alpha Middle");
        Thread.sleep(5L);
        final ChatSessionDto beta = service.createSession("Beta Newest");

        // Beta was updated most recently at creation
        Assertions.assertEquals(beta.id, service.getSessions().get(0).id);

        // Updating Zed makes it most recent despite alphabetical name
        Thread.sleep(5L);
        service.addMessage(zed.id, new ChatMessageDto("user", "Ping"));

        final List<ChatSessionDto> sortedAfterMessage = service.getSessions();
        Assertions.assertEquals(zed.id, sortedAfterMessage.get(0).id);

        // Reload from disk preserves recency order
        final AuraChatSessionService reloaded = new AuraChatSessionService(tempDir.toFile());
        Assertions.assertEquals(zed.id, reloaded.getSessions().get(0).id);

        // Renaming Alpha makes Alpha most recent
        Thread.sleep(5L);
        service.renameSession(alpha.id, "Alpha Updated");
        Assertions.assertEquals(alpha.id, service.getSessions().get(0).id);
    }

    @Test
    public void testGetSessionsEvictsExternallyDeletedFilesAndLeavesNoTempFiles() throws Exception
    {
        final AuraChatSessionService service = new AuraChatSessionService(tempDir.toFile());
        final ChatSessionDto s1 = service.createSession("Keep Me");
        final ChatSessionDto s2 = service.createSession("Remove Externally");
        service.addMessage(s1.id, new ChatMessageDto("user", "Hello"));

        try (final var paths = Files.list(tempDir))
        {
            final boolean hasTempFile = paths.anyMatch(p -> p.getFileName().toString().endsWith(".tmp"));
            Assertions.assertFalse(hasTempFile, "Atomic persistSession writes must leave no .tmp files behind");
        }

        // Delete s2 on disk externally; cached view must still reconcile diskIds cleanly
        Files.deleteIfExists(tempDir.resolve(s2.id + ".json"));
        final List<ChatSessionDto> remaining = service.getSessions();
        Assertions.assertEquals(1, remaining.size());
        Assertions.assertEquals(s1.id, remaining.get(0).id);
        Assertions.assertEquals(1, remaining.get(0).messages.size());
    }
}
