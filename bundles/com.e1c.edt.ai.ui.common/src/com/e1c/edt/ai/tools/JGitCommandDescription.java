/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Description of a Git command for documentation
 */
public class JGitCommandDescription
{
    private String description;
    private List<String> examples;
    private String notes;
    private List<CommandParameter> parameters;

    public JGitCommandDescription(String description)
    {
        this.description = description;
        this.parameters = new ArrayList<>();
    }

    public JGitCommandDescription(String description, List<String> examples)
    {
        this.description = description;
        this.examples = examples;
        this.parameters = new ArrayList<>();
    }

    public JGitCommandDescription(String description, List<String> examples, String notes)
    {
        this.description = description;
        this.examples = examples;
        this.notes = notes;
        this.parameters = new ArrayList<>();
    }

    public String getDescription()
    {
        return description;
    }

    public List<String> getExamples()
    {
        return examples;
    }

    public String getNotes()
    {
        return notes;
    }

    public List<CommandParameter> getParameters()
    {
        return parameters;
    }

    /**
     * Add a parameter to the command description
     */
    public JGitCommandDescription addParameter(String name, String description)
    {
        this.parameters.add(new CommandParameter(name, description));
        return this;
    }

    /**
     * Command parameter description
     */
    public static class CommandParameter
    {
        private final String name;
        private final String description;

        public CommandParameter(String name, String description)
        {
            this.name = name;
            this.description = description;
        }

        public String getName()
        {
            return name;
        }

        public String getDescription()
        {
            return description;
        }
    }
}
