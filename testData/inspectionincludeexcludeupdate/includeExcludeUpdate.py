from pydantic import BaseModel, Field

class A(BaseModel):
    a: int
    b: str

A(a=1, b='s').dict(include={'a', <error descr="Field 'c' not found in model 'A'">'c'</error>})
A(a=1, b='s').dict(exclude={'b', <error descr="Field 'd' not found in model 'A'">'d'</error>})
A(a=1, b='s').json(include={'a': True, <error descr="Field 'e' not found in model 'A'">'e'</error>: True})

class C(BaseModel):
    f: int = Field(alias='alias_f')
    
    class Config:
        allow_population_by_field_name = True

# include/exclude use internal field names
C(alias_f=1).dict(include={'f'})
# alias_f is not an internal field name
C(alias_f=1).dict(include={<error descr="Field 'alias_f' not found in model 'C'">'alias_f'</error>})

# update uses input names (dependent on config)
# allow_population_by_field_name = True -> both valid
C(alias_f=1).copy(update={'f': 2})
C(alias_f=1).copy(update={'alias_f': 2})
C(alias_f=1).copy(update={<error descr="Field 'invalid' not found in model 'C'">'invalid'</error>: 2})

class D(BaseModel):
    f: int = Field(alias='alias_f')
    # Default config: allow_population_by_field_name = False (V1)

# include/exclude use internal field names
D(alias_f=1).dict(include={'f'})
D(alias_f=1).dict(include={<error descr="Field 'alias_f' not found in model 'D'">'alias_f'</error>})

# update uses input names
# allow_population_by_field_name = False -> only alias valid
D(alias_f=1).copy(update={'alias_f': 2})
# 'f' is invalid for input if allow_population_by_field_name is False
D(alias_f=1).copy(update={<error descr="Field 'f' not found in model 'D'">'f'</error>: 2})
