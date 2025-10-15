from pydantic import BaseModel


class MyModel(BaseModel):
    """Test that private fields don't trigger 'Parameter unfilled' warnings."""
    
    # Public fields - should be required parameters
    public_field: str
    another_field: int
    
    # Private fields - should NOT be parameters
    _private_field: str = "private"
    _another_private: int = 42
    __double_private: float = 3.14
    
    # Private field without default - still shouldn't be a parameter
    _no_default: str


# This should only warn about missing public_field and another_field
MyModel(<warning descr="Parameter 'public_field' unfilled">)</warning>

# This should be valid - private fields are not parameters
MyModel(public_field="test", another_field=1)  # No warnings

# Private fields should not be accepted as parameters
MyModel(
    public_field="test",
    another_field=1,
    <warning descr="Unexpected argument">_private_field="should_warn"</warning>
)


class ModelWithOnlyPrivate(BaseModel):
    """Model with only private fields should have no required parameters."""
    _private1: str = "a"
    _private2: int = 1


# Should work without any parameters
ModelWithOnlyPrivate()  # No warnings


class MixedModel(BaseModel):
    """Test edge cases with mixed field types."""
    
    # Regular field
    name: str
    
    # Private with annotation
    _internal_id: int = 0
    
    # Private without annotation but with value
    _cache = {}
    
    # Class variable (not a field)
    class_var = "shared"


# Should only require 'name'
MixedModel(<warning descr="Parameter 'name' unfilled">)</warning>
MixedModel(name="test")  # No warnings