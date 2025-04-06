package com.townyblueprints.db;

import com.townyblueprints.models.PlacedBlueprint;
import java.util.Collection;

public sealed interface IStorage permits Database {
    void init();
    Collection<PlacedBlueprint> loadAllBlueprints();
    void saveBlueprint(PlacedBlueprint blueprint);
    void deleteBlueprint(String id);
}