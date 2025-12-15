import com.peosu.fn.ds.PathType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PathTypeTest {

    @Test
    public void testIntegerPattern() {
        assertTrue(PathType.INTEGER.getPattern().matcher("12345").matches());
        assertFalse(PathType.INTEGER.getPattern().matcher("123a45").matches());
    }

    @Test
    public void testRegularPathPattern() {
        assertTrue(PathType.REGULAR_PATH.getPattern().matcher("valid.path").matches());
        assertTrue(PathType.REGULAR_PATH.getPattern().matcher("\"valid.path\"").matches());
        assertFalse(PathType.REGULAR_PATH.getPattern().matcher("invalid..path").matches());
        assertFalse(PathType.REGULAR_PATH.getPattern().matcher("invalid path").matches());
    }

    @Test
    public void testArrayPattern() {
        assertTrue(PathType.ARRAY.getPattern().matcher("[1,2,3]").matches());
        assertTrue(PathType.ARRAY.getPattern().matcher("[1:5]").matches());
        assertTrue(PathType.ARRAY.getPattern().matcher("[*]").matches());
        assertTrue(PathType.ARRAY.getPattern().matcher("['name']").matches());
        assertTrue(PathType.ARRAY.getPattern().matcher("['na.me']").matches());
        assertTrue(PathType.ARRAY.getPattern().matcher("[\"na.me\"]").matches());
        assertTrue(PathType.ARRAY.getPattern().matcher("[`na.me`]").matches());
        assertTrue(PathType.ARRAY.getPattern().matcher("[?(some.expression)]").matches());
        assertFalse(PathType.ARRAY.getPattern().matcher("[name]").matches());
        assertTrue(PathType.ARRAY.getPattern().matcher("[1,2,]").matches());
    }


    @Test
    public void testGlobedPathPattern() {
        assertTrue(PathType.GLOBED_PATH.getPattern().matcher("*glob").matches());
        assertTrue(PathType.GLOBED_PATH.getPattern().matcher("glob*").matches());
        assertFalse(PathType.GLOBED_PATH.getPattern().matcher("normal.path").matches());
        assertFalse(PathType.GLOBED_PATH.getPattern().matcher("[1,2,3]").matches());
    }

    @Test
    public void testWildcardPattern() {
        assertTrue(PathType.WILDCARD.getPattern().matcher("...").matches());
        assertTrue(PathType.WILDCARD.getPattern().matcher("..valid.path").matches());
        assertFalse(PathType.WILDCARD.getPattern().matcher("valid..path").matches());
        assertFalse(PathType.WILDCARD.getPattern().matcher("normal.path").matches());
    }

    @Test
    public void testPathExpressionPattern() {
        assertTrue(PathType.PATH_EXPRESSION.getPattern().matcher("[?(expression)]").matches());
        assertTrue(PathType.PATH_EXPRESSION.getPattern().matcher("path[?(expression)]").matches());
        assertFalse(PathType.PATH_EXPRESSION.getPattern().matcher("normal.path").matches());
        assertFalse(PathType.PATH_EXPRESSION.getPattern().matcher("[1,2,3]").matches());
    }

    @Test
    public void testPathPossibilitiesPattern() {
        assertTrue(PathType.PATH_POSSIBILITIES.getPattern().matcher("valid.path").matches());
        assertTrue(PathType.PATH_POSSIBILITIES.getPattern().matcher("[1,2,3]").matches());
        assertTrue(PathType.PATH_POSSIBILITIES.getPattern().matcher("...").matches());
        assertTrue(PathType.PATH_POSSIBILITIES.getPattern().matcher("*glob").matches());
        assertFalse(PathType.PATH_POSSIBILITIES.getPattern().matcher("invalid path").matches());
    }

    @Test
    public void testJsonVariablePattern() {
        assertTrue(PathType.JSON_VARIABLE.getPattern().matcher("@.variable").matches());
        assertFalse(PathType.JSON_VARIABLE.getPattern().matcher("variable").matches());
        assertFalse(PathType.JSON_VARIABLE.getPattern().matcher("@variable").matches());
    }

    @Test
    public void testValuedTruePattern() {
        assertTrue(PathType.VALUED_TRUE.getPattern().matcher("true").matches());
        assertTrue(PathType.VALUED_TRUE.getPattern().matcher("YES").matches());
        assertTrue(PathType.VALUED_TRUE.getPattern().matcher("ndiyo").matches());
        assertFalse(PathType.VALUED_TRUE.getPattern().matcher("no").matches());
        assertFalse(PathType.VALUED_TRUE.getPattern().matcher("false").matches());
    }

    @Test
    public void testValuedFalsePattern() {
        assertTrue(PathType.VALUED_FALSE.getPattern().matcher("false").matches());
        assertTrue(PathType.VALUED_FALSE.getPattern().matcher("HAPANA").matches());
        assertTrue(PathType.VALUED_FALSE.getPattern().matcher("no").matches());
        assertFalse(PathType.VALUED_FALSE.getPattern().matcher("yes").matches());
        assertFalse(PathType.VALUED_FALSE.getPattern().matcher("true").matches());
    }

    @Test
    public void testNonePattern() {
        assertTrue(PathType.NONE.getPattern().matcher("null").matches());
        assertFalse(PathType.NONE.getPattern().matcher("undefined").matches());
        assertFalse(PathType.NONE.getPattern().matcher("").matches());
    }
}
