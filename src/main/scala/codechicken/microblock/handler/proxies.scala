package codechicken.microblock.handler

import java.util.function.Supplier
import java.util.{List => JList}

import codechicken.lib.config.ConfigFile
import codechicken.lib.gui.SimpleCreativeTab
import codechicken.lib.model.ModelRegistryHelper
import codechicken.lib.packet.PacketCustom
import codechicken.microblock._
import codechicken.multipart.handler.MultipartProxy._
import net.minecraft.client.renderer.ItemMeshDefinition
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.init.Blocks
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.{Item, ItemStack}
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraftforge.oredict.OreDictionary
import net.minecraftforge.registries.IForgeRegistry
import org.apache.logging.log4j.{LogManager, Logger}

import scala.collection.mutable

class MicroblockProxy_serverImpl {
    var logger: Logger = LogManager.getLogger("ForgeMicroBlockCBE")

    var microTab = new SimpleCreativeTab("microblockcbe", new Supplier[ItemStack] {
        override def get(): ItemStack = ItemMicroPart.create(0, 1, BlockMicroMaterial.materialKey(Blocks.STONE.getDefaultState))
    })

    var itemMicro: ItemMicroPart = _
    var sawStone: Item = _
    var sawIron: Item = _
    var sawDiamond: Item = _
    var stoneRod: Item = _

    var useSawIcons: Boolean = _
    var showAllMicroparts:Boolean = _

    def preInit() {
        itemMicro = new ItemMicroPart
        ForgeRegistries.ITEMS.register(itemMicro.setRegistryName("microblock"))
        itemMicro.setCreativeTab(microTab)
        sawStone = createSaw(config, "saw_stone", 1)
        sawIron = createSaw(config, "saw_iron", 2)
        sawDiamond = createSaw(config, "saw_diamond", 3)
        stoneRod = new Item().setTranslationKey("microblockcbe:stone_rod").setCreativeTab(CreativeTabs.MATERIALS)
        ForgeRegistries.ITEMS.register(stoneRod.setRegistryName("stone_rod"))

        OreDictionary.registerOre("rodStone", stoneRod)

        MinecraftForge.EVENT_BUS.register(MicroblockEventHandler)

        useSawIcons = config.getTag("useSawIcons").setComment("Set to true to use mc style icons for the saw instead of the 3D model").getBooleanValue(false)
        showAllMicroparts = config.getTag("showAllMicroparts").setComment("Set this to true to show all MicroParts in JEI. By default only Stone is shown.").getBooleanValue(false)
    }

    protected var saws = mutable.MutableList[Item]()

    def createSaw(config: ConfigFile, name: String, strength: Int) = {
        val saw = new ItemSaw(config.getTag(name).useBraces(), strength)
            .setTranslationKey("microblockcbe:" + name)
        ForgeRegistries.ITEMS.register(saw.setRegistryName(name))
        saws += saw
        saw
    }

    def addSawRecipe(saw: Item, blade: Item) {
        //        CraftingManager.getInstance.getRecipeList.add(
        //            new ShapedOreRecipe(new ItemStack(saw),
        //                "srr",
        //                "sbr",
        //                's': Character, "stickWood",
        //                'r': Character, "rodStone",
        //                'b': Character, blade))
    }

    def registerRecipes(registry: IForgeRegistry[IRecipe]) {
        registry.register(MicroRecipe.setRegistryName("micro_recipe"))
    }

    def init() {
        //        CraftingManager.getInstance.getRecipeList.add(MicroRecipe)
        //        CraftingManager.getInstance.addRecipe(new ItemStack(stoneRod, 4), "s", "s", 's': Character, Blocks.STONE)
        //        addSawRecipe(sawStone, Items.FLINT)
        //        addSawRecipe(sawIron, Items.IRON_INGOT)
        //        addSawRecipe(sawDiamond, Items.DIAMOND)
    }

    def postInit() {
        MicroMaterialRegistry.calcMaxCuttingStrength()
        PacketCustom.assignHandshakeHandler(MicroblockSPH.registryChannel, MicroblockSPH)
    }
}

class MicroblockProxy_clientImpl extends MicroblockProxy_serverImpl {
    @SideOnly(Side.CLIENT)
    override def preInit() {
        super.preInit()

        ModelRegistryHelper.registerItemRenderer(itemMicro, ItemMicroPartRenderer)
        registerFMPItemModel(stoneRod)
        saws.foreach(registerFMPItemModel)
        //saws.foreach(ModelRegistryHelper.registerItemRenderer(_, ItemSawRenderer))
    }

    @SideOnly(Side.CLIENT)
    def registerFMPItemModel(item: Item) {
        val loc = item.getRegistryName
        val mLoc = new ModelResourceLocation("microblockcbe:items", s"type=${loc.getPath}")
        ModelLoader.setCustomModelResourceLocation(item, 0, mLoc)
        ModelLoader.setCustomMeshDefinition(item, new ItemMeshDefinition {
            override def getModelLocation(stack: ItemStack) = mLoc
        })
    }

    @SideOnly(Side.CLIENT)
    override def postInit() {
        super.postInit()
        PacketCustom.assignHandler(MicroblockCPH.registryChannel, MicroblockCPH)
    }
}

object MicroblockProxy extends MicroblockProxy_clientImpl {
}
