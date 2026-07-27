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
package com.xceptance.neodymium.aura;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xceptance.neodymium.aura.dto.ChatMessageDto;
import com.xceptance.neodymium.aura.dto.ChatSessionDto;

/**
 * Thread-safe service managing chat session file persistence, CRUD operations,
 * message additions, and disk-backed serialization.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraChatSessionService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraChatSessionService.class);

    private final File chatHistoryDir;
    private final Gson gson;

    public AuraChatSessionService()
    {
        this.chatHistoryDir = new File(System.getProperty("user.dir"), "chat-history");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        ensureDirectoryExists();
    }

    private void ensureDirectoryExists()
    {
        if (!this.chatHistoryDir.exists())
        {
            this.chatHistoryDir.mkdirs();
        }
    }

    /**
     * Lists all persisted chat sessions, sorted by file name or modified timestamp.
     * Guaranteed to return at least one "Default Session" if none are found.
     */
    public synchronized List<ChatSessionDto> getSessions()
    {
        ensureDirectoryExists();
        final List<ChatSessionDto> sessions = new ArrayList<>();
        final File[] files = this.chatHistoryDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (files != null && files.length > 0)
        {
            for (final File file : files)
            {
                try
                {
                    final String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                    final ChatSessionDto session = this.gson.fromJson(content, ChatSessionDto.class);
                    if (session != null && session.id != null)
                    {
                        sessions.add(session);
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.error("[Aura Chat] Failed to load chat session file: {}", file.getName(), e);
                }
            }
        }

        // If no sessions exist, provision and return the default session
        if (sessions.isEmpty())
        {
            final ChatSessionDto defaultSession = createSession("Default Session");
            sessions.add(defaultSession);
        }

        // Sort by file name or ID to keep consistent listing order
        sessions.sort((s1, s2) -> s1.name.compareToIgnoreCase(s2.name));
        return sessions;
    }

    /**
     * Retrieves a specific chat session by ID.
     */
    public synchronized ChatSessionDto getSession(final String id)
    {
        if (id == null || id.isEmpty())
        {
            return getSessions().get(0);
        }

        final File file = new File(this.chatHistoryDir, id + ".json");
        if (file.exists())
        {
            try
            {
                final String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                final ChatSessionDto session = this.gson.fromJson(content, ChatSessionDto.class);
                if (session != null)
                {
                    return session;
                }
            }
            catch (final Exception e)
            {
                LOGGER.error("[Aura Chat] Failed to load chat session: {}", id, e);
            }
        }

        // Fallback to the first available session if ID is not found
        return getSessions().get(0);
    }

    /**
     * Creates and persists a new chat session on the server.
     */
    public synchronized ChatSessionDto createSession(final String name)
    {
        ensureDirectoryExists();
        final String cleanName = (name == null || name.trim().isEmpty()) ? "New Chat" : name.trim();
        final String id = "s-" + UUID.randomUUID().toString().substring(0, 8) + "-" + System.currentTimeMillis();
        
        final ChatSessionDto session = new ChatSessionDto(id, cleanName, new ArrayList<>());
        persistSession(session);
        return session;
    }

    /**
     * Renames an existing chat session.
     */
    public synchronized ChatSessionDto renameSession(final String id, final String newName)
    {
        if (id == null || newName == null || newName.trim().isEmpty())
        {
            return getSession(id);
        }

        final ChatSessionDto session = getSession(id);
        session.name = newName.trim();
        persistSession(session);
        return session;
    }

    /**
     * Deletes a chat session file.
     */
    public synchronized void deleteSession(final String id)
    {
        if (id == null || id.isEmpty())
        {
            return;
        }

        final File file = new File(this.chatHistoryDir, id + ".json");
        if (file.exists())
        {
            file.delete();
        }
    }

    /**
     * Appends a new chat message to a session and persists it.
     */
    public synchronized void addMessage(final String id, final ChatMessageDto message)
    {
        if (id == null || message == null)
        {
            return;
        }

        final ChatSessionDto session = getSession(id);
        session.messages.add(message);
        persistSession(session);
    }

    private void persistSession(final ChatSessionDto session)
    {
        ensureDirectoryExists();
        final File file = new File(this.chatHistoryDir, session.id + ".json");
        try
        {
            final String content = this.gson.toJson(session);
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        }
        catch (final IOException e)
        {
            LOGGER.error("[Aura Chat] Failed to write chat session file: {}", session.id, e);
        }
    }
}
