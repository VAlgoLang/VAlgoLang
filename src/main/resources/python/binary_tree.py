class Node:
    def __init__(self, text, color=RED, text_color=BLUE, line_color=GREEN, highlight_color=YELLOW, text_weight=NORMAL, font="Times New Roman", radius=0.6):
        self.circle = Circle(radius=radius, color=color)
        self.radius = radius
        self.text = Text(text, color=text_color)
        self.text_value = text
        self.left = None
        self.right = None
        self.rline = None
        self.lline = None
        self.text_weight = text_weight
        self.color = color
        self.line_color = line_color
        self.highlight_color = highlight_color
        self.font = font
        self.width = 1.5
        self.text_color = text_color
        self.circle_text = VGroup(self.circle, self.text)
        if len(text) > 1:
            self.text.set_width(0.6 * self.circle.get_width())
        self.all = VGroup(self.circle_text)

    def set_left(self, node, scale):
        if self.left is not None:
            self.all.remove(self.left.all)

        self.left = node
        return self.set_left_mobject(node.circle, node.all, scale)

    def set_left_mobject(self, shape, vgroup, scale):
        vgroup.next_to(self.circle_text, np.add(DOWN * 2 * scale, self.width * LEFT))
        animations = []
        if self.lline is None:
            line = Line(self.circle.point_at_angle(np.deg2rad(225)), shape.point_at_angle(np.deg2rad(90)),
                        color=self.line_color)
            self.lline = line
            self.all.add(self.lline)
            animations.append(ShowCreation(self.lline))
        else:
            self.lline.set_start_and_end_attrs(self.circle.point_at_angle(np.deg2rad(225)),
                                               shape.point_at_angle(np.deg2rad(90)))

        self.all.add(self.left.all)
        return animations

    def set_right(self, node, scale):
        if self.right is not None:
            self.all.remove(self.right.all)

        self.right = node
        return self.set_right_mobject(node.circle, node.all, scale)

    def set_right_mobject(self, shape, vgroup, scale):
        vgroup.next_to(self.circle_text, np.add(DOWN * 2 * scale, self.width * RIGHT))
        animations = []
        if self.rline is None:
            line = Line(self.circle.point_at_angle(np.deg2rad(315)), shape.point_at_angle(np.deg2rad(90)),
                        color=self.line_color)
            self.rline = line
            self.all.add(self.rline)
            animations.append(ShowCreation(self.rline))
        else:
            self.rline.set_start_and_end_attrs(self.circle.point_at_angle(np.deg2rad(315)),
                                               shape.point_at_angle(np.deg2rad(90)))

        self.all.add(self.right.all)
        return animations

    def set_reference(self, tree, scale, left):
        if left:
            if self.left is not None:
                self.all.remove(self.left.all)
        else:
            if self.right is not None:
                self.all.remove(self.right.all)

        # For pointer angle
        dummy = Node("", radius=0)
        rectangle = Rectangle_block(tree.identifier, color=self.color, text_color=self.text_color, font=self.font)

        if left:
            self.left = rectangle
            animation = self.set_left_mobject(dummy.circle, dummy.circle_text, scale)
        else:
            self.right = rectangle
            animation = self.set_right_mobject(dummy.circle, dummy.circle_text, scale)

        rectangle.all.move_to(dummy.all, UP)
        return animation

    def edit_node_value(self, text):
        new_text_obj = Text(text, color=self.text_color, width=0.6 * self.circle.get_width())
        animation = [Transform(self.text, new_text_obj.move_to(self.circle_text.get_center()))]
        return animation

    def highlight(self, color):
        return [ApplyMethod(self.circle_text.set_color, color)]

    def unhighlight(self):
        return [ApplyMethod(self.circle.set_color, self.color), ApplyMethod(self.text.set_color, self.text_color)]

    def set_radius(self, new_radius):
        self.all.scale(new_radius / self.radius)
        self.radius = new_radius

    # delete left assumes tree has left child
    def delete_left(self):
        if self.left is None:
            return []

        animation = [FadeOut(self.left.all)]
        self.all.remove(self.left.all, self.lline)
        self.left = None
        self.lline = None
        return animation

    # delete right assumes tree has right child
    def delete_right(self):
        if self.right is None:
            return []

        animation = [FadeOut(self.right.all)]
        self.all.remove(self.right.all, self.rline)
        self.right = None
        self.rline = None
        return animation

    def clean_up(self):
        return [FadeOut(self.all)]


class Tree(DataStructure, ABC):
    def __init__(self, ul, ur, ll, lr, root, identifier, color=RED, text_color=BLUE, text_weight=NORMAL,
                 font="Times New Roman", radius=0.6):
        self.margin = [0.2, 0.2, 0]
        super().__init__(np.add(ul, self.margin), np.subtract(ur, self.margin), np.add(ll, self.margin),
                         np.subtract(lr, self.margin), np.average([ul, ur, ll, lr], axis=0), color, text_color, text_weight, font)
        self.identifier = identifier
        self.radius = radius
        self.max_radius = radius * 1.3
        self.text_padding = 0.3
        self.scale = 1
        self.root = root
        self.all.add(self.root.all)

    def create_init(self, n):
        name = Text(self.identifier)
        name.next_to(self.root.circle_text, UP, self.text_padding)
        self.all.add(name)
        return ApplyMethod(self.all.move_to, self.aligned_edge)

    def update_root(self, node):
        animations = self.root.delete_left() + self.root.delete_right() + [
            ReplacementTransform(self.root.circle_text, node.circle_text.move_to(self.root.circle.get_center())),
            FadeIn(node.all)]
        self.root = node
        return animations

    # Checks whether adding child will cross boundary
    def check_if_child_will_cross_boundary(self, parent, child, is_left):
        child.set_radius(self.radius)
        x, y, _ = parent.circle_text.get_center()
        y_child = y - (2 * self.scale)
        x_child = x - 1.5 if is_left else x + 1.5

        x_prev, y_prev = x_child, y_child
        curr = child.right
        right_most_x, right_most_y = x_child, y_child
        while curr is not None:
            curr.set_radius(self.radius)
            right_most_y = y_prev - (2 * self.scale)
            right_most_x = x_prev + 1.5
            # bounds.append((x_child_2, y_child_2))
            x_prev, y_prev = right_most_x, right_most_y
            curr = curr.right

        x_prev, y_prev = x_child, y_child
        curr = child.left
        left_most_x, left_most_y = x_child, y_child
        while curr is not None:
            curr.set_radius(self.radius)
            left_most_x = x_prev - 1.5
            left_most_y = y_prev - (2 * self.scale)
            # bounds.append((x_child_2, y_child_2))
            x_prev, y_prev = left_most_x, left_most_y
            curr = curr.left

        animations = []

        is_within_left_or_right_boundary = left_most_x > self.ll[0] and right_most_x < self.lr[0] and right_most_y > self.ll[1] and left_most_y > self.ll[1]

        if (not is_within_left_or_right_boundary) and (self.will_cross_boundary(abs(x - left_most_x), "LEFT") or self.will_cross_boundary(abs(x - right_most_x),
                                                                                                                                          "RIGHT")
                                                       or self.will_cross_boundary(y - min(right_most_y, left_most_y), "BOTTOM")):
            group_left_x = self.all.get_left()[0]
            group_right_x = self.all.get_right()[0]
            group_top_y = self.all.get_top()[1]
            group_bottom_y = self.all.get_bottom()[1]
            width = group_right_x - group_left_x
            height = group_top_y - group_bottom_y
            if right_most_x > group_right_x:
                width += abs((right_most_x - group_right_x))
            if left_most_x < group_left_x:
                width += abs(group_left_x - left_most_x)
            if min(left_most_y, right_most_y) < group_bottom_y:
                height += abs(min(left_most_y, right_most_y))
            scale_animation, scale_factor = self.shrink2(width + MED_SMALL_BUFF, height + MED_SMALL_BUFF)

            self.scale = scale_factor
            self.radius = self.radius * scale_factor
            if scale_animation:
                animations.extend(scale_animation)
                corner_coord = self.ur[0] - (scale_factor *self.all.get_width()) / 2 if is_left else self.ul[0] + (scale_factor *self.all.get_width()) / 2
                animations.append(ApplyMethod(self.all.move_to, np.array([corner_coord, self.ul[1] - (self.all.get_height()/ 2) , 0])))
        return animations

    # Assumes parent is in the tree
    def set_right(self, parent, child):
        child.set_radius(self.radius)
        animations = parent.set_right(child, self.scale)
        return self._resize_after_modification(animations)

    # Assumes parent is in the tree
    def set_left(self, parent, child):
        child.set_radius(self.radius)
        animations = parent.set_left(child, self.scale)
        return self._resize_after_modification(animations)

    # Assumes parent is in the tree
    def delete_left(self, parent):
        animations = parent.delete_left()
        return self._resize_after_modification(animations)

    # Assumes parent is in the tree
    def delete_right(self, parent):
        animations = parent.delete_right()
        return self._resize_after_modification(animations)

    def edit_node_value(self, node, text):
        return node.edit_node_value(text)

    def set_reference_right(self, parent, tree):
        animations = parent.set_reference(tree, self.scale, left=False)
        return self._resize_after_modification(animations)

    def set_reference_left(self, parent, tree):
        animations = parent.set_reference(tree, self.scale, left=True)
        return self._resize_after_modification(animations)

    def _resize_after_modification(self, animations):
        scale_animations, scale_factor = self.check_positioning()
        animations.extend(scale_animations)

        if scale_factor != self.scale:
            target_radius = self.radius * (scale_factor if scale_factor else 1)
            self.scale = (scale_factor if scale_factor else 1)
            self.radius = target_radius
        return animations

    # Assumes node is in the tree
    def check_positioning(self):
        animations = []
        overlapping_children, scale = self.check_overlapping_children(self.root)

        if len(overlapping_children) > 0:
            animations.extend(overlapping_children)
        else:
            shrink, scale_factor = self.shrink_if_cross_border()
            if not shrink:
                grow, scale = self.grow_if_small()
                if grow:
                    animations.extend(grow)

        return animations, scale

     # Assumes node is in the tree
    def crossing_bottom_border(self):
        curr_top = self.all.get_top()[1]
        bottom_bound = self.ll[1]
        curr_bottom = self.all.get_bottom()[1]
        target_height = curr_top - bottom_bound
        overflow_height = curr_top - curr_bottom
        scale = target_height / overflow_height
        return scale


    # Assumes node is in the tree
    def crossing_left_right_border(self, offset_x, scale=10e9):
        target_width = abs(self.lr[0] - self.ll[0])
        overflow_width = self.all.get_width() + 2 * offset_x
        scale = min(scale, target_width / overflow_width)
        return scale

    def grow_if_small(self):
        target_width = abs(self.lr[0] - self.ll[0])
        target_height = abs(self.lr[1] - self.ul[1])
        curr_width = self.all.get_width()
        curr_height = self.all.get_height() + self.text_padding + 0.1
        scale = min(target_width / curr_width, target_height / curr_height)
        if scale >= 1:
            # check radius size
            target_radius = self.radius * scale
            if(target_radius > self.max_radius):
                scale *= self.max_radius / target_radius

            return [ScaleInPlace(self.all, scale), ApplyMethod(self.all.move_to, np.array([(self.ul[0] + self.ur[0]) / 2,(self.ll[1] + self.ur[1]) / 2, 0]))], scale
        else:
            return 0, self.scale

    def shrink_if_cross_border(self, offset_x=0):
        # Check if crossing bottom
        if self.all.get_bottom()[1] < self.ll[1]:
            scale = self.crossing_bottom_border()
        else:
            scale = 10e9

        # Check if crossing left and right borders
        if self.all.get_left()[0] - offset_x < self.ll[0] or self.all.get_right()[0] + offset_x> self.lr[0]:
            scale = self.crossing_left_right_border(offset_x, scale=scale)

        if scale == 10e9:
                return 0, self.scale
        else :
                return [ScaleInPlace(self.all, scale), ApplyMethod(self.all.move_to, self.aligned_edge)], scale


    def check_overlapping_children(self, node):
        if node is None:
            return [], self.scale

        if node.left is not None and node.right is not None:
            overlap_x = node.left.all.get_right()[0] - node.right.all.get_left()[0] + self.margin[0] * 2
            if overlap_x > 0:
                offset_x = overlap_x / 2
                left_offset = LEFT * offset_x
                right_offset = RIGHT * offset_x
                new_rline = Line(node.circle.point_at_angle(np.deg2rad(315)),
                                np.add(node.right.circle.point_at_angle(np.deg2rad(90)), right_offset),
                                color=node.line_color)
                new_lline = Line(node.circle.point_at_angle(np.deg2rad(225)),
                                np.add(node.left.circle.point_at_angle(np.deg2rad(90)), left_offset),
                                color=node.line_color)

                animations = [
                    ApplyMethod(node.left.all.move_to, np.add(node.left.all.get_center(), left_offset)),
                    Transform(node.lline, new_lline),
                    ApplyMethod(node.right.all.move_to, np.add(node.right.all.get_center(), right_offset)),
                    Transform(node.rline, new_rline),
                ]

                shrink, scale = self.shrink_if_cross_border(offset_x)
                if shrink:
                    animations.extend(shrink)

                return animations, scale
            else:
                return [], self.scale

        animations, left_scale = self.check_overlapping_children(node.left)
        right_animations, right_scale = self.check_overlapping_children(node.right)
        animations.extend(right_animations)

        return animations, min(left_scale, right_scale)

    def clean_up(self):
        return [FadeOut(self.all)]