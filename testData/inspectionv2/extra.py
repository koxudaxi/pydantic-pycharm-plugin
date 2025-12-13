from pydantic import BaseModel, ConfigDict


class A(BaseModel):
    model_config = ConfigDict(extra="forbid")
    model_233: str
    model_id: int
    name: str


# model_233, model_id are valid fields - no errors
a = A(model_233="bert", model_id=1, name="test")

# b is an undefined field - error
A(model_233="bert", model_id=1, name="test", <error descr="'b' extra fields not permitted">b='123'</error>)
