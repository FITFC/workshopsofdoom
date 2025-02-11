package commoble.workshopsofdoom.features;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.features.SpawnEntityFeature.EntityConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class SpawnEntityFeature extends Feature<EntityConfig>
{
	public SpawnEntityFeature(Codec<EntityConfig> codec)
	{
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<EntityConfig> context)
	{
		EntityConfig config = context.config();
		WorldGenLevel level = context.level();
		Level serverLevel = level.getLevel();
		Entity entity = config.entityType.create(serverLevel);
		if (entity == null)
			return false;
		
		BlockPos pos = context.origin();
		RandomSource rand = context.random();

		entity.moveTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, Mth.wrapDegrees(rand.nextFloat() * 360.0F), 0.0F);
		// this bit comes from EntityType::spawn
		config.getNBT().ifPresent(nbt -> {
			CompoundTag entityNbt = entity.saveWithoutId(new CompoundTag());
            UUID uuid = entity.getUUID();
            entityNbt.merge(nbt);
            entity.setUUID(uuid);
            entity.load(entityNbt);
		});
		if (entity instanceof Mob)
		{
			Mob mob = (Mob) entity;
			// if we don't enable persistance
			// then the entity will despawn instantly
			// as structures generate well outside of the instant-despawn range
			// so there's no point in making transient entities via structure generation
			mob.setPersistenceRequired();
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, (SpawnGroupData)null, entity.serializeNBT());
            
		}
		// add entity and any riders
		level.addFreshEntityWithPassengers(entity);
		
		return true;
	}

	public static class EntityConfig implements FeatureConfiguration
	{
		@SuppressWarnings("deprecation")
		public static final Codec<EntityConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Registry.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(EntityConfig::getEntityType),
				CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(EntityConfig::getNBT)).
			apply(instance, EntityConfig::new));

		private final EntityType<?> entityType;
		private final @Nullable Optional<CompoundTag> nbt;

		public EntityConfig(EntityType<?> entityType, Optional<CompoundTag> nbt)
		{
			this.entityType = entityType;
			this.nbt = nbt;
		}

		public EntityType<?> getEntityType()
		{
			return this.entityType;
		}

		public Optional<CompoundTag> getNBT()
		{
			return this.nbt;
		}
	}
}
