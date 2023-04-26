package tools.redstone.redstonetools.features.commands;

import tools.redstone.redstonetools.features.Feature;
import tools.redstone.redstonetools.features.arguments.Argument;
import tools.redstone.redstonetools.features.feedback.Feedback;
import tools.redstone.redstonetools.utils.DirectionArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.Nullable;

import static tools.redstone.redstonetools.features.arguments.DirectionSerializer.direction;
import static tools.redstone.redstonetools.features.arguments.serializers.IntegerSerializer.integer;
import static tools.redstone.redstonetools.utils.DirectionUtils.directionToBlock;
import static tools.redstone.redstonetools.utils.DirectionUtils.matchDirection;


@Feature(name = "RStack", description = "Stacks with custom distance", command = "/rstack")
public class RStackFeature extends CommandFeature {
    public static final Argument<Integer> count = Argument
            .ofType(integer())
            .withDefault(1);

    public static final Argument<DirectionArgument> direction = Argument
            .ofType(direction())
            .withDefault(DirectionArgument.ME);

    public static final Argument<Integer> spacing = Argument
            .ofType(integer())

            .withDefault(2);

    @Override
    protected Feedback execute(ServerCommandSource source) throws CommandSyntaxException {
        var actor = FabricAdapter.adaptPlayer(source.getPlayer());

        var localSession = WorldEdit.getInstance()
                .getSessionManager()
                .get(actor);

        final var selectionWorld = localSession.getSelectionWorld();
        assert selectionWorld != null;

        final Region selection;
        try {
            selection = localSession.getSelection(selectionWorld);
        } catch (IncompleteRegionException ex) {
            actor.printError(TextComponent.of("Please make a selection with worldedit first."));
            return Feedback.error("No selection found!");
        }

        final Mask airFilter = new Mask() {
            @Override
            public boolean test(BlockVector3 vector) {
                return !"minecraft:air".equals(selectionWorld.getBlock(vector).getBlockType().getId());
            }

            @Nullable
            @Override
            public Mask2D toMask2D() {
                return null;
            }
        };

        var playerFacing = actor.getLocation().getDirectionEnum();
        var stackDirection = matchDirection(direction.getValue(), playerFacing);

        var stackVector = directionToBlock(stackDirection);


        try (var editSession = localSession.createEditSession(actor)) {
            for (var i = 1; i <= count.getValue(); i++) {
                var copy = new ForwardExtentCopy(
                        editSession,
                        selection,
                        editSession,
                        selection.getMinimumPoint().add(stackVector.multiply(i * spacing.getValue()))
                );
                copy.setSourceMask(airFilter);
                Operations.complete(copy);
            }
            localSession.remember(editSession);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }

        return Feedback.success("Stacked " + count.getValue() + " time(s)");
    }
}
