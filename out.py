import tempfile
from abc import ABC, abstractmethod
from manimlib.imports import *
 
class Main(Scene):
    code_start = 0
    code_end = 10
    def construct(self):
        # Building partition of scene
        width = FRAME_WIDTH
        height = FRAME_HEIGHT
        lhs_width = width * 1/3
        rhs_width = width * 2/3
        variable_height = (height - SMALL_BUFF) * 1/3
        code_height = (height - SMALL_BUFF) * 2/3
        variable_frame = Rectangle(height=variable_height, width=lhs_width, color=BLACK)
        variable_frame.to_corner(UL, buff=0)
        code_frame = Rectangle(height=code_height, width=lhs_width, color=BLACK)
        code_frame.next_to(variable_frame, DOWN, buff=0) 

        # Building variable visualisation pane
        variable_block = Variable_block([""], variable_frame)
        variable_vg = variable_block.build()
        variable_vg.move_to(variable_frame)
        self.play(FadeIn(variable_vg), run_time=1.0)
        # Building code visualisation pane
        code_lines = [["let original = ","Array<Array<number>>(2,3){[1,2,3],[4,5,6]};"]]
        code_block = Code_block(code_lines, syntax_highlighting=True, syntax_highlighting_style="monokai", tab_spacing=1)
        code_text = code_block.build()
        code_text.set_width(4.2)
        code_text.next_to(variable_frame, DOWN, buff=0.9)
        code_text.to_edge(buff=MED_LARGE_BUFF)
        self.code_end = len(code_lines) if self.code_end > len(code_lines) else self.code_end
        self.play(FadeIn(code_text[self.code_start:self.code_end]), run_time=1.0)
        # Constructing current line pointer
        pointer = ArrowTip(color=YELLOW).scale(0.7).flip(TOP)
        self.play(*variable_block.update_variable([]), run_time=1.0)
        # Moves the current line pointer to line 1
        self.move_arrow_to_line(1, pointer, code_block, code_text)
        # Constructing new Array<number> "original"
        array = Array2D([[ "1.0","2.0","3.0"],[ "4.0","5.0","6.0"]], "original", [(0.0, 0.0),(0.0, 0.0),(0.0, 0.0),(0.0, 0.0)])
        
        array.build("Write")
        self.play(*variable_block.update_variable([]), run_time=1.0)
        self.wait(1.0)

    def place_at(self, group, x, y):
        group.to_edge(np.array([x, y, 0]))
    
    def move_relative_to_edge(self, group, x, y):
        self.play(ApplyMethod(group.to_edge, np.array([x, y, 0])))
    
    def move_relative_to_obj(self, group, target, x, y):
        self.play(ApplyMethod(group.next_to, target, np.array([x, y, 0])))
    
    def place_relative_to_obj(self, group, target, x, y):
        group.next_to(target, np.array([x, y, 0]))
    
    def fade_out_if_needed(self, mobject):
        if mobject in self.mobjects:
            return FadeOut(mobject)
        else:
            return None
    
    def move_arrow_to_line(self, line_number, pointer, code_block, code_text):
        idx = 0
        for i in range(line_number):
            idx += len(code_block.code[i])
    
        if idx > self.code_end:
            animation = self.fade_out_if_needed(pointer)
            if animation is not None:
                self.play(animation, runtime=0.1)
            self.scroll_down(code_text, (idx - self.code_end))
        elif idx - 1 < self.code_start:
            animation = self.fade_out_if_needed(pointer)
            if animation is not None:
                self.play(animation, runtime=0.1)
            self.scroll_up(code_text, (self.code_start - idx+len(code_block.code[line_number-1])))
    
        line_object = code_block.get_line_at(line_number)
        self.play(FadeIn(pointer.next_to(line_object, LEFT, MED_SMALL_BUFF)))
    
    def scroll_down(self, group, scrolls):
        sh_val = group[self.code_start].get_corner(UP + LEFT)[1] - group[self.code_start + 1].get_corner(UP + LEFT)[1]
        for i in range(1, 1 + scrolls):
            group[self.code_end + i - 1].next_to(group[self.code_end - 2 + i], DOWN, aligned_edge=LEFT)
            self.play(FadeOut(group[self.code_start + i - 1]), FadeIn(group[self.code_end + i - 1]),
                      group[(self.code_start + i):(self.code_end + i)].shift, sh_val * UP, run_time=0.1)
        self.code_start = self.code_start + scrolls
        self.code_end = self.code_end + scrolls
    
    def scroll_up(self, group, scrolls):
        sh_val = group[self.code_start].get_corner(UP + LEFT)[1] - group[self.code_start + 1].get_corner(UP + LEFT)[1]
        for i in range(1, 1 + scrolls):
            group[self.code_start - i].next_to(group[self.code_start - i + 1], UP, aligned_edge=LEFT)
            # self.play(ReplacementTransform())
            self.play(FadeOut(group[self.code_end - i]), FadeIn(group[self.code_start - i]),
                      group[(self.code_start - i):(self.code_end - i)].shift, sh_val * DOWN, run_time=0.1)
        self.code_start = self.code_start - scrolls
        self.code_end = self.code_end - scrolls
# Object representing the visualised variables on the top left hand side of the screen
class Variable_block:
    def __init__(self, variables, variable_frame, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        group = VGroup()
        for v in variables:
            text = Text(v, color=text_color, weight=text_weight, font=font)
            text.set_width(min(0.8 * variable_frame.get_width(), text.get_width()))
            group.add(text)
        self.group = group
        self.variable_frame = variable_frame
        self.text_color = text_color
        self.text_weight = text_weight
        self.font = font
        self.size = len(variables)

    def build(self):
        self.group.arrange(DOWN, aligned_edge=LEFT)
        return self.group.move_to(self.variable_frame)

    def update_variable(self, variables):
        # To avoid awkward replace transform
        if self.size == 0:
            self.group.scale_in_place(0)

        self.size = len(variables)
        if self.size == 0:
            return [FadeOut(self.group)]
        group = VGroup()
        for v in variables:
            text = Text(v, color=self.text_color, weight=self.text_weight, font=self.font)
            text.set_width(min(0.8 * self.variable_frame.get_width(), text.get_width()))
            group.add(text)

        old_group = self.group
        self.group = group
        self.group.arrange(DOWN, aligned_edge=LEFT)
        self.group.move_to(self.variable_frame)
        return [
            ReplacementTransform(old_group, group)
        ]
class Code_block:
    def __init__(self, code, syntax_highlighting=True, syntax_highlighting_style="inkpot", text_color=WHITE, text_weight=NORMAL, font="Times New Roman", tab_spacing=2):
        group = VGroup()

        if syntax_highlighting:
            fp = tempfile.NamedTemporaryFile(suffix='.re')

            for c in code:
                for sc in c:
                    fp.write(bytes(sc + "\n", encoding='utf-8'))

            fp.seek(0)

            self.paragraph = Code(fp.name, style=syntax_highlighting_style, language="reasonml", line_spacing=0.2,
                              tab_width=tab_spacing).code
            fp.close()
            group.add(self.paragraph)
            group.set_width(5)
            self.all = self.paragraph
        else:
            for c in code:
                for sc in c:
                    text = Text(sc, color=text_color, weight=text_weight, font=font)
                    group.add(text)
            group.set_width(5)
            self.all = group

        self.code = code


    def build(self):
        return self.all.arrange(DOWN, aligned_edge=LEFT, center=True)

    def get_line_at(self, line_number):
        idx = 0
        for i in range(line_number):
            idx += len(self.code[i])
        return self.all[idx-1]
class DataStructure(ABC):

    def __init__(self, ul, ur, ll, lr, aligned_edge, color=WHITE, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.ul = ul
        self.ur = ur
        self.ll = ll
        self.lr = lr
        self.max_width = self.lr[0] - self.ul[0]
        self.width_center = self.ul[0] + self.max_width / 2
        self.max_height = self.ul[1] - self.lr[1]
        self.aligned_edge = aligned_edge
        self.color = color
        self.text_color = text_color
        self.text_weight = text_weight
        self.font = font
        self.all = VGroup()

    def shrink(self, new_width, new_height):
        scale_factor = min((self.max_width - 2 * MED_SMALL_BUFF) / new_width, (self.max_height - 2 * MED_SMALL_BUFF) / new_height)
        if scale_factor != 1:
            return ApplyMethod(self.all.scale, scale_factor, {"about_edge": self.aligned_edge}), scale_factor
        return 0, 1

    def shrink2(self, new_width, new_height):
        scale_factor = min((self.max_width - 2 * MED_SMALL_BUFF) / new_width,
                           (self.max_height - 2 * MED_SMALL_BUFF) / new_height)
        if scale_factor != 1:
            return [ScaleInPlace(self.all, scale_factor),
                    ApplyMethod(self.all.move_to, self.aligned_edge)], scale_factor
        return 0, 1

    def will_cross_boundary(self, object_dim, boundary_name):
        boundary_options = {"TOP": self.will_cross_top_boundary,
                            "RIGHT": self.will_cross_right_boundary,
                            "BOTTOM": self.will_cross_bottom_boundary,
                            "LEFT": self.will_cross_left_boundary}
        return boundary_options[boundary_name](object_dim)

    def will_cross_top_boundary(self, object_height):
        frame_top_y = self.ul[1]
        group_top_y = self.all.get_top()[1]
        return group_top_y + object_height > frame_top_y

    def will_cross_bottom_boundary(self, object_height):
        frame_bottom_y = self.ll[1]
        group_bottom_y = self.all.get_bottom()[1]
        return group_bottom_y - object_height < frame_bottom_y

    def will_cross_right_boundary(self, object_width):
        frame_right_x = self.lr[0]
        group_right_x = self.all.get_right()[0]
        return group_right_x + object_width > frame_right_x

    def will_cross_left_boundary(self, object_width):
        frame_left_x = self.ll[0]
        group_left_x = self.all.get_left()[0]
        return group_left_x - object_width < frame_left_x

    def has_crossed_top_boundary(self):
        frame_top_y = self.ul[1]
        group_top_y = self.all.get_top()[1]
        return group_top_y > frame_top_y

    def add(self, obj):
        self.all.add(obj)

    @abstractmethod
    def create_init(self, ident):
        pass

    @abstractmethod
    def shrink_if_cross_border(self, obj):
        pass
class Rectangle_block:
    def __init__(self, text, target=None, height=0.75, width=1.5, color=BLUE, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.text = Text(text, color=text_color, weight=text_weight, font=font)
        self.shape = Rectangle(height=height, width=width, color=color)
        self.all = VGroup(self.text, self.shape)
        self.text.set_width(7/10 * width)
        self.height = height
        if(self.text.get_height() > 0.6 * height):
            self.text.scale(0.6 * height / self.text.get_height())
        self.width = width
        self.text_color = text_color
        self.font = font
        self.pointer = Triangle(color=color,fill_color=color,fill_opacity=1).flip(LEFT).scale(0.1)
        self.pointer.next_to(self.shape, TOP, 0.01)
        if target:
            self.owner = target
            self.all.scale(max(target.empty.get_height() / self.shape.get_height(), target.empty.get_width() / self.shape.get_width()))

    def replace_text(self, new_text, color=None):
        if not color:
            color = self.text_color
        new_text_obj = Text(new_text, color=color, font=self.font)
        new_text_obj.set_width(self.width * 7/10)
        if(new_text_obj.get_height() > 0.6 * self.height):
            new_text_obj.scale(0.6 * self.height / new_text_obj.get_height())
        return (Transform(self.text, new_text_obj.move_to(self.all.get_center())))

class Array:
    def __init__(self, values, title, boundaries, color=BLUE, text_color=WHITE, padding=True):
        self.values = values
        boundary_width = boundaries[1][0] - boundaries[0][0]
        boundary_height = boundaries[0][1] - boundaries[3][1]

        title_width = 1 if title != "" else 0
        width_per_element = (boundary_width - title_width) / len(values)
        self.padding = 0.2 if padding else 0

        square_dim = min((boundaries[0][1] - boundaries[3][1] - self.padding), width_per_element)
        self.array_elements = [
            Rectangle_block(str(val), color=color, text_color=text_color, width=square_dim, height=square_dim) for val
            in self.values]
        offset = 0
        if ((square_dim * len(values)) + title_width) < boundary_width:
            offset = (boundary_width - ((square_dim * len(values)) + title_width)) / 2

        self.title = VGroup(Text(title).set_width(title_width))
        if title_width != 0 and self.title.get_height() > 0.5 * boundary_height:
            self.title.scale(0.5 * boundary_height / self.title.get_height())

        self.title.move_to(
            np.array([boundaries[0][0] + (title_width / 2) + offset, (boundaries[0][1] + boundaries[3][1]) / 2, 0]))
        self.color = color


    def build(self):
        previous = self.title
        buff = self.padding
        for array_elem in self.array_elements:
            group = array_elem.all
            group.next_to(previous, RIGHT, buff)
            previous = group
            buff = 0
        return self

    def swap_mobjects(self, i1: int, i2: int):
        o1 = self.array_elements[i1].text
        o2 = self.array_elements[i2].text
        o1_copy = deepcopy(o1)
        o2_copy = deepcopy(o2)
        o1_copy.move_to(o2.get_center())
        o2_copy.move_to(o1.get_center())
        self.array_elements[i1].text = o2
        self.array_elements[i2].text = o1
        return [
            CounterclockwiseTransform(o1, o1_copy),
            CounterclockwiseTransform(o2, o2_copy)
        ]

    def clone_and_swap(self, i1, i2):
        elem1_copy = deepcopy(self.array_elements[i1].text)
        elem2_copy = deepcopy(self.array_elements[i2].text)
        elem2_copy2 = deepcopy(self.array_elements[i2].text)
        elem2_copy2.move_to(self.array_elements[i1].text.get_center())
        elem1_copy2 = deepcopy(elem1_copy)
        elem1_copy2.move_to(self.array_elements[i2].text.get_center())
        return elem1_copy, elem2_copy, [[ApplyMethod(elem1_copy.next_to, self.array_elements[i1].all, np.array([0, 0.4, 0]))],
                                        [ClockwiseTransform(elem2_copy, elem2_copy2), FadeOut(self.array_elements[i1].text)],
                                        [ClockwiseTransform(elem1_copy, elem1_copy2), FadeOut(self.array_elements[i2].text)]]

class Array2D:
    def __init__(self, values, title, boundaries, color=BLUE, text_color=WHITE):
        self.values = values
        boundary_width = boundaries[1][0] - boundaries[0][0]
        title_width = 1 if title != "" else 0
        width_per_element = (boundary_width - title_width - 0.2) / len(values[0])
        boundary_height = boundaries[0][1] - boundaries[3][1]
        square_dim = min((boundary_height - 1) / len(values), width_per_element)
        self.rows = []
        offset_from_bottom = (boundary_height - square_dim * len(values)) / 2
        sub_array_width = (square_dim * len(values[0]))
        for i in range(len(values) - 1, -1, -1):
            new_ll = boundaries[2][0] + ((boundary_width - sub_array_width) /2) , boundaries[2][1] + (i * square_dim) + offset_from_bottom - 0.25
            new_boundaries = [(new_ll[0], new_ll[1] + square_dim),
                              (new_ll[0] + sub_array_width, new_ll[1] + square_dim), new_ll,
                              (new_ll[0] + sub_array_width, new_ll[1])]
            self.rows.append(Array(values[len(values) - 1 - i], "",new_boundaries, color=color, text_color=text_color, padding=False).build())
        self.title = VGroup(Text(title).set_width(title_width))
        self.title.move_to(
            np.array([boundaries[0][0] + (boundary_width/ 2), (boundaries[0][1] - 0.5), 0]))
        self.color = color
        self.text_color = text_color

    def build(self, creation_style=None):
        if not creation_style:
            creation_style = "FadeIn"
        creation_transform = globals()[creation_style]
        animations = []
        for i in range(len(self.rows)):
            animations += [creation_transform(array_elem.all) for array_elem in self.rows[i].array_elements]
        return animations

    def replace_row(self, row_index, new_values):
        return [self.rows[row_index].array_elements[i].replace_text(str(v)) for i, v in enumerate(new_values)]

    def swap_mobjects(self, i1, j1, i2, j2):
        # Animations for fading to grey and fading back to original color
        fade_to_grey_animations = []
        fade_to_original_animations = []
        for i in range(len(self.rows)):
            for j in range(len(self.rows[0].array_elements)):
                if (i != i1 or j != j1) and (i != i2 or j != j2):
                    fade_to_grey_animations.append(FadeToColor(self.rows[i].array_elements[j].text, GREY))
                    fade_to_original_animations.append(FadeToColor(self.rows[i].array_elements[j].text, self.text_color))

        # Swapping elements
        o1 = self.rows[i1].array_elements[j1].text
        o2 = self.rows[i2].array_elements[j2].text
        o1_copy = deepcopy(o1)
        o2_copy = deepcopy(o2)
        o1_copy.move_to(o2.get_center())
        o2_copy.move_to(o1.get_center())
        self.rows[i1].array_elements[j1].text = o2
        self.rows[i2].array_elements[j2].text = o1
        swap_animations = [CounterclockwiseTransform(o1, o1_copy), CounterclockwiseTransform(o2, o2_copy)]

        return [fade_to_grey_animations, swap_animations, fade_to_original_animations]