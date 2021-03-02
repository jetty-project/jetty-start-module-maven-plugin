package org.mortbay.jetty.maven.modulestart;

import java.util.ArrayList;
import java.util.List;

public class ModFileSection
{
    private String sectionName;

    private List<String> lines = new ArrayList<>();

    public ModFileSection(String sectionName)
    {
        this.sectionName = sectionName;
    }

    public String getSectionName()
    {
        return sectionName;
    }

    public void setSectionName(String sectionName)
    {
        this.sectionName = sectionName;
    }

    public void addLine(String line)
    {
        this.lines.add(line);
    }

    public List<String> getLines()
    {
        return lines;
    }

    public void setLines(List<String> lines)
    {
        this.lines = lines;
    }
}
