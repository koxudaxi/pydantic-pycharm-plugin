from pydantic import BaseModel


class E(BaseModel, <error descr="Specifying config in two places is ambiguous, use either Config attribute or class kwargs">allow_mutation=True</error>):
    class <error descr="Specifying config in two places is ambiguous, use either Config attribute or class kwargs">Config</error>:
        allow_mutation= True

class F(BaseModel, allow_mutation=True):
    pass

class G(BaseModel):
    class Config:
        allow_mutation=True
